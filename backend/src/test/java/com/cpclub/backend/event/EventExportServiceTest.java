package com.cpclub.backend.event;

import com.cpclub.backend.event.dto.EventAttendeeDto;
import com.cpclub.backend.event.service.EventExportService;
import com.cpclub.backend.user.entity.AcademicYear;
import com.cpclub.backend.user.entity.ClubRole;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The attendance spreadsheet.
 *
 * <p>Every assertion reads the produced workbook back rather than inspecting the
 * service, because the file is the deliverable: what the club opens is what these
 * tests describe.</p>
 *
 * <p>The sheet carries six columns, not the whole member record. It is shared with
 * the department and with faculty, so it holds what an attendance register needs
 * and nothing else -- no phone numbers, no social links.</p>
 */
class EventExportServiceTest {

    private final EventExportService service = new EventExportService();

    private static final String[] EXPECTED_HEADERS = {
            "Name", "Codeforces Profile", "Email", "Student ID", "Year", "Added At"
    };

    @Test
    @DisplayName("The header row is the agreed six columns, in order")
    void exportToExcel_writesTheAgreedHeaders() throws IOException {
        byte[] bytes = service.exportToExcel(List.of(attendee()));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row header = workbook.getSheetAt(0).getRow(0);

            assertEquals(EXPECTED_HEADERS.length, header.getLastCellNum(), "column count changed");
            for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
                assertEquals(EXPECTED_HEADERS[i], header.getCell(i).getStringCellValue(),
                        "header " + i + " changed");
            }
        }
    }

    @Test
    @DisplayName("Each attendee becomes one row beneath the header")
    void exportToExcel_writesOneRowPerAttendee() throws IOException {
        byte[] bytes = service.exportToExcel(List.of(attendee(), attendee(), attendee()));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            // Row 0 is the header, so the last row index equals the attendee count.
            assertEquals(3, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    @Test
    @DisplayName("The student ID is the part of the university address before the @")
    void exportToExcel_writesTheStudentIdFromTheEmail() throws IOException {
        byte[] bytes = service.exportToExcel(List.of(attendee()));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row row = workbook.getSheetAt(0).getRow(1);

            assertEquals("202401226@dau.ac.in", row.getCell(2).getStringCellValue());
            assertEquals("202401226", row.getCell(3).getStringCellValue());
        }
    }

    @Test
    @DisplayName("An address with no @ has no student ID to report, and stays blank")
    void exportToExcel_leavesTheStudentIdBlankForAnOddAddress() throws IOException {
        EventAttendeeDto odd = new EventAttendeeDto(
                9L, "Odd", "not-an-address",
                null, false, null,
                null, null, null, null,
                null, null, null, null,
                null, AcademicYear.FIRST_YEAR, LocalDateTime.of(2026, 3, 1, 10, 0));

        byte[] bytes = service.exportToExcel(List.of(odd));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row row = workbook.getSheetAt(0).getRow(1);
            assertTrue(row.getCell(3) == null || row.getCell(3).toString().isEmpty(),
                    "student ID should be blank, was: " + row.getCell(3));
        }
    }

    @Test
    @DisplayName("Codeforces is written as a link, since this sheet gets shared")
    void exportToExcel_writesTheCodeforcesProfileAsALink() throws IOException {
        byte[] bytes = service.exportToExcel(List.of(attendee()));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("https://codeforces.com/profile/ravi_cf",
                    workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
        }
    }

    @Test
    @DisplayName("The year reads as words, because people read this sheet")
    void exportToExcel_writesTheYearInWords() throws IOException {
        EventAttendeeDto fresher = attendeeWithYear(AcademicYear.FIRST_YEAR);
        EventAttendeeDto senior = attendeeWithYear(AcademicYear.SECOND_YEAR_ONWARDS);

        byte[] bytes = service.exportToExcel(List.of(fresher, senior));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("1st year", sheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("2nd year onwards", sheet.getRow(2).getCell(4).getStringCellValue());
        }
    }

    @Test
    @DisplayName("A member who answered nothing exports blank cells, not the text null")
    void exportToExcel_leavesMissingValuesBlank() throws IOException {
        EventAttendeeDto sparse = new EventAttendeeDto(
                7L, "Newcomer", "new@dau.ac.in",
                null, false, null,
                null, null, null, null,
                null, null, null, null,
                null, null, LocalDateTime.of(2026, 3, 1, 10, 0));

        byte[] bytes = service.exportToExcel(List.of(sparse));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row row = workbook.getSheetAt(0).getRow(1);

            assertEquals("Newcomer", row.getCell(0).getStringCellValue());
            // No Codeforces handle and no year: blank rather than "null".
            for (int column : new int[]{1, 4}) {
                if (row.getCell(column) != null) {
                    assertTrue(row.getCell(column).toString().isEmpty(),
                            "column " + column + " should be blank, was: " + row.getCell(column));
                }
            }
        }
    }

    @Test
    @DisplayName("An event nobody attended still produces a readable workbook")
    void exportToExcel_handlesAnEmptyList() throws IOException {
        byte[] bytes = service.exportToExcel(List.of());

        assertNotNull(bytes);
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(0, sheet.getLastRowNum(), "only the header row should exist");
            assertEquals("Name", sheet.getRow(0).getCell(0).getStringCellValue());
        }
    }

    // ------------------------------------------------- the same sheet, as rows

    @Test
    @DisplayName("The row grid opens with the same six headers as the workbook")
    void toSheetRows_startsWithTheHeaderRow() {
        List<List<String>> rows = service.toSheetRows(List.of(attendee()));

        assertIterableEquals(List.of(EXPECTED_HEADERS), rows.get(0));
    }

    @Test
    @DisplayName("Each attendee becomes one row, in column order")
    void toSheetRows_writesTheColumnsInOrder() {
        List<List<String>> rows = service.toSheetRows(List.of(attendee()));

        assertIterableEquals(List.of(
                "Ravi",
                "https://codeforces.com/profile/ravi_cf",
                "202401226@dau.ac.in",
                "202401226",
                "2nd year onwards",
                "2026-03-01 10:00"
        ), rows.get(1));
    }

    @Test
    @DisplayName("Missing values are empty strings, never null")
    void toSheetRows_usesEmptyStringsForMissingValues() {
        // This grid is serialized to JSON for the browser, which hands it to the
        // Google Sheets API. A null there is a gap the API skips, which would
        // shift every later column left and quietly misfile the whole sheet.
        EventAttendeeDto sparse = new EventAttendeeDto(
                7L, "Newcomer", "new@dau.ac.in",
                null, false, null,
                null, null, null, null,
                null, null, null, null,
                null, null, LocalDateTime.of(2026, 3, 1, 10, 0));

        List<String> row = service.toSheetRows(List.of(sparse)).get(1);

        assertEquals(EXPECTED_HEADERS.length, row.size(), "a column went missing");
        for (int column = 0; column < row.size(); column++) {
            assertNotNull(row.get(column), "column " + column + " was null");
        }
        assertEquals("", row.get(1), "no Codeforces handle");
        assertEquals("", row.get(4), "no year");
    }

    @Test
    @DisplayName("An event nobody attended is the header row alone")
    void toSheetRows_handlesAnEmptyList() {
        List<List<String>> rows = service.toSheetRows(List.of());

        assertEquals(1, rows.size());
        assertIterableEquals(List.of(EXPECTED_HEADERS), rows.get(0));
    }

    private EventAttendeeDto attendee() {
        return attendeeWithYear(AcademicYear.SECOND_YEAR_ONWARDS);
    }

    private EventAttendeeDto attendeeWithYear(AcademicYear year) {
        return new EventAttendeeDto(
                1L, "Ravi", "202401226@dau.ac.in",
                "9876543210", true, "https://cdn/avatar.png",
                "ravi_cf", 1900, "ravi_lc", 1750,
                "https://codechef.com/users/ravi", "https://atcoder.jp/users/ravi",
                "https://github.com/ravi", "https://linkedin.com/in/ravi",
                ClubRole.CORE, year, LocalDateTime.of(2026, 3, 1, 10, 0));
    }
}
