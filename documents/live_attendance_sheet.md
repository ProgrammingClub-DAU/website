# Live attendance sheet: setup

One Google spreadsheet for the club that keeps itself up to date. Each event gets
its own tab, named `#<event id> <event title>`. A few seconds after an admin marks
someone present or removes them, the server rewrites that event's tab.

The columns are the same six as the Excel export: name, Codeforces profile, email,
student ID, year, time added. There are no phone numbers.

This is separate from the **Google Sheets** button on the event page. That button
makes a one-off copy in the Drive of whoever clicks it. The live sheet is a single
shared spreadsheet that the server keeps current.

The feature is **off** until both environment variables in step 4 are set. If
they're missing, nothing else on the site changes.

---

## 1. Create the service account

A service account is a Google account that belongs to the Cloud project rather
than to a person. The server signs in as this account to write the sheet.

1. Open https://console.cloud.google.com and select **the same project** that
   holds the sign-in Client ID.
2. Go to **IAM & Admin > Service Accounts > + Create service account**.
   - Name: `attendance-sheet`
   - Click **Create and continue**.
   - Roles: **leave empty**. The account needs no access to the project, only to
     the one spreadsheet you share with it.
   - Click **Done**.
3. Open the new account, go to the **Keys** tab and choose
   **Add key > Create new key > JSON > Create**. A `.json` file downloads.

**Treat that file like a password.** Anyone who has it can edit every
spreadsheet shared with this account. Don't commit it, don't paste it into chat,
and delete your local copy once step 4 is done.

If Google says **"Service account key creation is disabled"**, the project sits
under an organisation whose policy blocks keys. Either ask that organisation's
admin to allow it for this project, or use a project created from a personal
account.

## 2. Check the Sheets API is on

Go to **APIs & Services > Library > Google Sheets API**. It should say **Manage**.
If it says **Enable**, click it. The Google Sheets export button already needed
this, so it's probably on.

## 3. Create the spreadsheet and share it

1. Open https://sheets.new and name the file, for example
   `PC DAU - Live attendance`.
2. Click **Share** and paste the service account's email address. It's the
   `client_email` value in the JSON file and ends with
   `iam.gserviceaccount.com`.
3. Set its role to **Editor**, untick **Notify people**, and click **Share**.
4. Share the file with the other admins in the same way. **Do not** turn on
   "Anyone with the link": the sheet contains student emails.

## 4. Set the variables on Render

On Render, open the backend service and go to **Environment > Add environment
variable**:

| Key | Value |
|---|---|
| `ATTENDANCE_SPREADSHEET_ID` | The spreadsheet's URL from the address bar, or just the id inside it (`/d/<id>/edit`) |
| `GOOGLE_SERVICE_ACCOUNT_JSON` | The **entire contents** of the downloaded JSON file |

Save. Render redeploys on its own.

If Render mangles the JSON (it contains line breaks inside the key), paste
Base64 of the file instead. Both forms are accepted.

- Linux or macOS: `base64 -w0 key.json` (on macOS: `base64 -i key.json`)
- PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("key.json"))`

## 5. Check it works

1. Open any event in **Admin > Events**. A **Live attendance sheet** panel
   appears above the attendance section.
2. Mark someone present. After about 5 seconds, click **Open sheet**. The event's
   tab is there with the new row.
3. For events whose attendance was taken before this was set up, open each one
   and click **Sync now**.

## If the panel shows an error

The panel repeats the server's message, which says what to fix:

| Message | Fix |
|---|---|
| Google refused access to the spreadsheet | Share the spreadsheet with the service account's address as **Editor** (step 3) |
| The spreadsheet was not found | `ATTENDANCE_SPREADSHEET_ID` is wrong, or the file was deleted |
| Google rejected the service-account key | The key was deleted or revoked. Create a new one (step 1.3) and update the variable |
| The Google Sheets API is turned off | Step 2 |
| ... is not set on the server | One of the two variables in step 4 is missing |

After fixing the problem, click **Sync now** to retry immediately.

## Good to know

- **Each update rewrites the whole tab** from the database. A removed attendee
  disappears, and a missed update is corrected by the next one.
- **Anything you type into an event's tab will be overwritten** by the next
  update. Keep notes in a separate tab. Tabs that don't start with `#<number>`
  are never touched.
- **Renaming an event renames its tab.** The tab is found by the `#<id>` at the
  start of its name, so don't edit that part.
- **Deleting an event leaves its tab** in place as a record. Delete the tab by
  hand if you don't want it.
- **A member who changes their name** appears with the new name at the event's
  next update, or when you click Sync now.
- **If the key ever leaks**, go to **IAM & Admin > Service Accounts >
  attendance-sheet > Keys**, delete it, then create a new key and update the
  Render variable.
