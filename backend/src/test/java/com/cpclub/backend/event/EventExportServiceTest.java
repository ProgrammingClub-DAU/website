package com.cpclub.backend.event;

import com.cpclub.backend.event.dto.EventAttendeeDto;
import com.cpclub.backend.event.service.EventExportService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The attendance export.
 *
 * <p>Every assertion reads the produced workbook back rather than inspecting the
 * service. The output is a file the club opens in Excel, so the only thing worth
 * asserting is what that file contains.</p>
 */
class EventExportServiceTest {

    private final EventExportService service = new EventExportService();

    private static final String[] EXPECTED_HEADERS = {
            "ID", "Name", "Email", "Phone Number", "Club Role", "Avatar URL",
            "CF Handle", "CF Rating", "LeetCode Handle", "LeetCode Rating",
            "CodeChef URL", "AtCoder URL", "GitHub", "LinkedIn", "Added At"
    };

    @Test
    @DisplayName("The header row is the agreed fifteen columns, in order")
    void exportToExcel_writesTheAgreedHeaders() throws IOException {
        byte[] bytes = service.exportToExcel(List.of(attendee()));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row header = workbook.getSheetAt(0).getRow(0);

            assertEquals(EXPECTED_HEADERS.length, header.getLastCellNum(),
                    "column count changed");
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
            Sheet sheet = workbook.getSheetAt(0);
            // Row 0 is the header, so the last row index equals the attendee count.
            assertEquals(3, sheet.getLastRowNum());
        }
    }

    @Test
    @DisplayName("A member with nothing linked exports blank cells, not the text null")
    void exportToExcel_leavesMissingValuesBlank() throws IOException {
        EventAttendeeDto sparse = new EventAttendeeDto(
                7L, "Newcomer", "new@dau.ac.in",
                null, false, null,
                null, null, null, null,
                null, null, null, null,
                null, LocalDateTime.of(2026, 3, 1, 10, 0));

        byte[] bytes = service.exportToExcel(List.of(sparse));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row row = workbook.getSheetAt(0).getRow(1);

            assertEquals("Newcomer", row.getCell(1).getStringCellValue());
            // Phone, club role and every handle column: absent rather than "null".
            for (int column : new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13}) {
                if (row.getCell(column) != null) {
                    assertTrue(row.getCell(column).toString().isEmpty(),
                            "column " + column + " should be blank, was: " + row.getCell(column));
                }
            }
        }
    }

    @Test
    @DisplayName("Ratings are written as numbers so the sheet can sort and total them")
    void exportToExcel_writesRatingsAsNumbers() throws IOException {
        byte[] bytes = service.exportToExcel(List.of(attendee()));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Row row = workbook.getSheetAt(0).getRow(1);
            assertEquals(1900d, row.getCell(7).getNumericCellValue());
            assertEquals(1750d, row.getCell(9).getNumericCellValue());
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
            assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
        }
    }

    private EventAttendeeDto attendee() {
        return new EventAttendeeDto(
                1L, "Alice", "alice@dau.ac.in",
                "9876543210", true, "https://cdn/avatar.png",
                "alice_cf", 1900, "alice_lc", 1750,
                "https://codechef.com/users/alice", "https://atcoder.jp/users/alice",
                "https://github.com/alice", "https://linkedin.com/in/alice",
                ClubRole.CORE, LocalDateTime.of(2026, 3, 1, 10, 0));
    }
}
