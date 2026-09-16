package com.cpclub.backend.event.service;

import com.cpclub.backend.user.entity.AcademicYear;
import com.cpclub.backend.event.dto.EventAttendeeDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds an event's attendance sheet.
 *
 * <p>Six columns, and deliberately not the whole member record: the sheet is
 * shared with the department and with faculty, so it carries what an attendance
 * register needs and nothing else -- no phone numbers, no social links, no
 * ratings.</p>
 *
 * <p>Two things consume this. {@link #exportToExcel} produces the .xlsx download,
 * and {@link #toSheetRows} produces the same grid as plain text for the browser
 * to write into Google Sheets. Both go through {@code toSheetRows}, so the two
 * exports cannot drift into disagreeing about what the attendance sheet is --
 * which is exactly what would happen if the Sheets version reimplemented the
 * column rules in TypeScript.</p>
 */
@Service
@Slf4j
public class EventExportService {

    /**
     * Column headers, in order.
     *
     * <p>The array is the single definition of both the header row and the column
     * count: the row builder below emits one value per header, so adding a column
     * here and forgetting the corresponding value would be a visible blank rather
     * than a silent misalignment of every later column.</p>
     */
    private static final String[] HEADERS = {
            "Name", "Codeforces Profile", "Email", "Student ID", "Year", "Added At"
    };

    /** Written as text so the sheet reads the same in every locale. */
    private static final DateTimeFormatter ADDED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * The attendance sheet as a grid of strings, header row first.
     *
     * <p>Everything is text, including the timestamp. A sheet that is read rather
     * than calculated with is better off saying exactly what was stored than
     * letting each client guess a date format.</p>
     *
     * <p>Missing values are the empty string, never null: this grid is serialized
     * to JSON for the browser, and a null there becomes a gap the Google Sheets
     * API would silently skip, shifting every later column left.</p>
     *
     * @param attendees the event's attendance list, in display order
     * @return the header row followed by one row per attendee
     */
    public List<List<String>> toSheetRows(List<EventAttendeeDto> attendees) {
        List<List<String>> rows = new ArrayList<>(attendees.size() + 1);
        rows.add(Arrays.asList(HEADERS));

        for (EventAttendeeDto attendee : attendees) {
            rows.add(rowFor(attendee));
        }

        return rows;
    }

    /**
     * Builds the .xlsx download.
     *
     * @param attendees the event's attendance list, in display order
     * @return the .xlsx file as bytes
     * @throws UncheckedIOException if the workbook cannot be serialized
     */
    public byte[] exportToExcel(List<EventAttendeeDto> attendees) {
        List<List<String>> rows = toSheetRows(attendees);

        // try-with-resources on both: a workbook holds native buffers, and leaking
        // one per export is a slow memory leak in a long-running server.
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Attendees");
            CellStyle headerStyle = boldStyle(workbook);

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                List<String> values = rows.get(rowIndex);

                for (int column = 0; column < values.size(); column++) {
                    Cell cell = row.createCell(column);
                    cell.setCellValue(values.get(column));
                    if (rowIndex == 0) {
                        cell.setCellStyle(headerStyle);
                    }
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            // Nothing here talks to a disk or a network: this is a write into a
            // byte array, so an IOException means the workbook itself is broken.
            // Wrapped rather than swallowed, because returning an empty or partial
            // spreadsheet would look like an event nobody attended.
            log.error("Failed to build the attendee export", e);
            throw new UncheckedIOException("Failed to build the attendee export", e);
        }
    }

    /**
     * One attendee, in column order.
     *
     * @param attendee the attendee
     * @return one value per header, empty strings for what is missing
     */
    private List<String> rowFor(EventAttendeeDto attendee) {
        return List.of(
                orEmpty(attendee.name()),
                codeforcesProfile(attendee.codeforcesHandle()),
                orEmpty(attendee.email()),
                studentId(attendee.email()),
                yearLabel(attendee.academicYear()),
                attendee.addedAt() != null ? ADDED_AT_FORMAT.format(attendee.addedAt()) : ""
        );
    }

    private CellStyle boldStyle(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }

    /**
     * The student ID, taken from the part of the address before the @.
     *
     * <p>Members sign in with their university address, so 202401226@dau.ac.in
     * carries the ID the department files attendance under. An address in another
     * shape has no student ID to report, and leaves the cell blank rather than
     * inventing one.</p>
     *
     * @param email the member's address
     * @return the local part, or an empty string when there is nothing usable
     */
    private String studentId(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        return at <= 0 ? "" : email.substring(0, at);
    }

    /** A clickable profile rather than a bare handle, since this sheet gets shared. */
    private String codeforcesProfile(String handle) {
        return handle == null || handle.isBlank()
                ? ""
                : "https://codeforces.com/profile/" + handle.trim();
    }

    /** Words rather than the enum name, because people read this sheet. */
    private String yearLabel(AcademicYear year) {
        if (year == null) {
            return "";
        }
        return year == AcademicYear.FIRST_YEAR ? "1st year" : "2nd year onwards";
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
