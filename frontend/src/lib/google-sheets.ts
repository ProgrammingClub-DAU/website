/**
 * Writing an attendance sheet straight into the admin's own Google Drive.
 *
 * The server is not involved beyond handing over the rows. The browser asks
 * Google for a short-lived token scoped to `drive.file`, creates the spreadsheet
 * with it, and hands back the URL. Three things follow from that, and all three
 * are why it was built this way:
 *
 * - No service-account key sits in the backend's environment waiting to leak.
 * - No Google client libraries are loaded on a 512 MB instance with a 160m
 *   Metaspace cap that has already died of exactly that once.
 * - The spreadsheet belongs to the person who exported it, in their Drive, under
 *   their quota. Nobody has to be given access to a shared club account.
 *
 * `drive.file` is the narrowest scope that can do this: it grants access only to
 * files this application itself created, never to anything already in the
 * person's Drive. It is also a non-sensitive scope, so Google does not require
 * the app to pass its verification review.
 */

import {
  GOOGLE_CLIENT_ID,
  loadGoogleIdentityServices,
  type GoogleTokenResponse,
} from "@/lib/google-sign-in";

/** Files this app made, and nothing else in the person's Drive. */
const DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file";

const SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

/** The tab the rows are written to, named rather than left as "Sheet1". */
const TAB_TITLE = "Attendees";

/** The part of Sheets' create response this uses. */
interface SpreadsheetCreated {
  spreadsheetId: string;
  spreadsheetUrl: string;
  sheets?: { properties?: { sheetId?: number } }[];
}

/**
 * Raised for anything the person can act on, so callers can show `.message`
 * directly instead of guessing at a generic failure.
 */
export class GoogleSheetExportError extends Error {}

/**
 * A prepared exporter, holding Google's token client.
 *
 * The client is kept behind this rather than handed out raw, because the
 * callback-swapping it needs per request is nobody else's business.
 */
export interface SheetExporter {
  /**
   * Opens Google's consent popup and resolves with a Drive token.
   *
   * Must be called from a click handler with no `await` before it, or the
   * popup is blocked.
   */
  requestToken(): Promise<string>;
}

/**
 * Prepares the token client, ahead of the click that needs it.
 *
 * Worth doing on mount rather than on click. Requesting a token opens a popup,
 * and browsers only allow that while a user gesture is still fresh -- if the GIS
 * script had to be fetched first, the gesture would have expired by the time the
 * popup was asked for and it would be blocked.
 *
 * @returns an exporter ready to request a token synchronously
 */
export async function prepareSheetExport(): Promise<SheetExporter> {
  if (!GOOGLE_CLIENT_ID) {
    throw new GoogleSheetExportError(
      "Google is not configured for this deployment, so the export cannot run.",
    );
  }

  await loadGoogleIdentityServices();

  const oauth2 = window.google?.accounts?.oauth2;
  if (!oauth2) {
    throw new GoogleSheetExportError("Could not load Google's authorisation library.");
  }

  // Google's client takes its callbacks once, at construction, but each export
  // needs its own promise to settle. These two slots are what the client calls;
  // requestToken points them at the current attempt. Re-initialising instead
  // would cost another round trip and lose the click's popup allowance.
  let onToken: (response: GoogleTokenResponse) => void = () => {};
  let onError: (error: { type?: string; message?: string }) => void = () => {};

  const client = oauth2.initTokenClient({
    client_id: GOOGLE_CLIENT_ID,
    scope: DRIVE_FILE_SCOPE,
    callback: (response) => onToken(response),
    error_callback: (error) => onError(error),
  });

  return {
    requestToken() {
      return new Promise<string>((resolve, reject) => {
        onToken = (response) => {
          if (response.access_token) {
            resolve(response.access_token);
          } else {
            reject(
              new GoogleSheetExportError(
                "Google did not grant access to Drive, so the sheet was not created.",
              ),
            );
          }
        };

        onError = (error) => {
          // Closing the popup is the ordinary case and is not worth an alarming
          // message; a blocked popup needs the person to do something.
          reject(
            new GoogleSheetExportError(
              error.type === "popup_failed_to_open"
                ? "Google's window was blocked. Allow popups for this site and try again."
                : "Export cancelled.",
            ),
          );
        };

        client.requestAccessToken();
      });
    },
  };
}
/**
 * Creates a spreadsheet in the signed-in person's Drive and fills it.
 *
 * @param accessToken a `drive.file` token from {@link requestDriveToken}
 * @param title the spreadsheet's name
 * @param rows the whole grid, header row first
 * @returns the URL of the new spreadsheet
 */
export async function createSpreadsheet(
  accessToken: string,
  title: string,
  rows: string[][],
): Promise<string> {
  const headers = {
    Authorization: `Bearer ${accessToken}`,
    "Content-Type": "application/json",
  };

  const created = await googleFetch<SpreadsheetCreated>(SHEETS_API, {
    method: "POST",
    headers,
    body: JSON.stringify({
      properties: { title },
      sheets: [{ properties: { title: TAB_TITLE } }],
    }),
  });

  const { spreadsheetId, spreadsheetUrl } = created;
  const sheetId = created.sheets?.[0]?.properties?.sheetId;

  // RAW, not USER_ENTERED: a student ID like 202401226 must stay the text it is.
  // USER_ENTERED would have Sheets parse it as a number, strip a leading zero on
  // some IDs, and switch it to scientific notation on others.
  await googleFetch<unknown>(
    `${SHEETS_API}/${spreadsheetId}/values/${encodeURIComponent(TAB_TITLE)}!A1?valueInputOption=RAW`,
    {
      method: "PUT",
      headers,
      body: JSON.stringify({ values: rows }),
    },
  );

  if (sheetId !== undefined) {
    // Cosmetic, and deliberately not fatal: a sheet with the right rows and a
    // plain header is still the export. Failing the whole thing over formatting
    // would be a poor trade.
    await formatHeader(spreadsheetId, sheetId, headers).catch(() => {});
  }

  return spreadsheetUrl;
}

/** Bold the header and freeze it, so scrolling a long list keeps the columns named. */
async function formatHeader(
  spreadsheetId: string,
  sheetId: number,
  headers: Record<string, string>,
): Promise<void> {
  await googleFetch<unknown>(`${SHEETS_API}/${spreadsheetId}:batchUpdate`, {
    method: "POST",
    headers,
    body: JSON.stringify({
      requests: [
        {
          repeatCell: {
            range: { sheetId, startRowIndex: 0, endRowIndex: 1 },
            cell: { userEnteredFormat: { textFormat: { bold: true } } },
            fields: "userEnteredFormat.textFormat.bold",
          },
        },
        {
          updateSheetProperties: {
            properties: { sheetId, gridProperties: { frozenRowCount: 1 } },
            fields: "gridProperties.frozenRowCount",
          },
        },
        { autoResizeDimensions: { dimensions: { sheetId, dimension: "COLUMNS" } } },
      ],
    }),
  });
}

/**
 * Calls a Google API and turns a failure into something readable.
 *
 * Google's errors arrive as a nested JSON envelope that says nothing useful when
 * logged as a status code alone. The one worth naming is 403 on a fresh project,
 * which almost always means the Sheets API was never switched on.
 */
async function googleFetch<T>(url: string, init: RequestInit): Promise<T> {
  const response = await fetch(url, init);

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as
      | { error?: { message?: string; status?: string } }
      | null;
    const detail = body?.error?.message ?? `${response.status} ${response.statusText}`;

    if (response.status === 403 && /api|disabled|enable/i.test(detail)) {
      throw new GoogleSheetExportError(
        "The Google Sheets API is not enabled on the club's Google Cloud project. " +
          "Enable it in APIs & Services > Library, then try again.",
      );
    }

    throw new GoogleSheetExportError(`Google refused the request: ${detail}`);
  }

  return (await response.json()) as T;
}
