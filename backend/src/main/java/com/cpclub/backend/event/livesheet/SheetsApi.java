package com.cpclub.backend.event.livesheet;

import java.util.List;

/**
 * The three Google Sheets operations the live attendance sheet needs.
 *
 * <p>An interface so the sync logic can be tested without Google: the tests
 * drive an in-memory spreadsheet, and only {@link RestSheetsApi} speaks HTTP.</p>
 */
public interface SheetsApi {

    /**
     * One tab of a spreadsheet.
     *
     * @param sheetId Google's numeric id for the tab, stable across renames
     * @param title the name on the tab
     * @param rowCount rows the tab currently has room for
     * @param columnCount columns the tab currently has room for
     */
    record SheetTab(int sheetId, String title, int rowCount, int columnCount) {
    }

    /**
     * Every tab in the spreadsheet.
     *
     * @param spreadsheetId the spreadsheet
     * @return its tabs, in order
     */
    List<SheetTab> listTabs(String spreadsheetId);

    /**
     * Adds an empty tab.
     *
     * @param spreadsheetId the spreadsheet
     * @param title the new tab's name, which must not already be taken
     * @param rowCount rows to make room for
     * @param columnCount columns to make room for
     * @return the new tab
     */
    SheetTab addTab(String spreadsheetId, String title, int rowCount, int columnCount);

    /**
     * Replaces a tab's contents with {@code rows}, renaming it to {@code title}.
     *
     * <p>All or nothing: either the tab ends up holding exactly these rows, or
     * it is left as it was.</p>
     *
     * @param spreadsheetId the spreadsheet
     * @param tab the tab to overwrite
     * @param title the name the tab should have afterwards
     * @param rows the full contents, header row first
     */
    void replaceTab(String spreadsheetId, SheetTab tab, String title, List<List<String>> rows);
}
