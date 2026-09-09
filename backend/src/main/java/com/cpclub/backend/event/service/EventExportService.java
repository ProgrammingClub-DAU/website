package com.cpclub.backend.event.service;

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
import java.util.List;

/**
 * Renders an event's attendance list as an .xlsx workbook.
 *
 * <p>The club uses this sheet outside the site — mailing lists, certificates,
 * reports to the department — so the columns are the whole member record rather
 * than the few fields the admin table shows.</p>
 */
@Service
@Slf4j
public class EventExportService {

    /**
     * Column headers, in order.
     *
     * <p>The array is the single definition of both the header row and the column
     * count: the writer below indexes cells off it, so adding a column here and
     * forgetting the corresponding value would be a visible blank rather than a
     * silent misalignment of every later column.</p>
     */
    private static final String[] HEADERS = {
            "ID", "Name", "Email", "Phone Number", "Club Role", "Avatar URL",
            "CF Handle", "CF Rating", "LeetCode Handle", "LeetCode Rating",
            "CodeChef URL", "AtCoder URL", "GitHub", "LinkedIn", "Added At"
    };

    /** Written as text so the sheet reads the same in every locale. */
    private static final DateTimeFormatter ADDED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Builds the workbook.
     *
     * @param attendees the event's attendance list, in display order
     * @return the .xlsx file as bytes
     * @throws UncheckedIOException if the workbook cannot be serialized
     */
    public byte[] exportToExcel(List<EventAttendeeDto> attendees) {
        // try-with-resources on both: a workbook holds native buffers, and leaking
        // one per export is a slow memory leak in a long-running server.
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Attendees");
            writeHeaderRow(workbook, sheet);

            int rowIndex = 1;
            for (EventAttendeeDto attendee : attendees) {
                writeAttendeeRow(sheet.createRow(rowIndex++), attendee);
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
     * Writes the bold header row.
     *
     * @param workbook workbook being built, needed to create the font
     * @param sheet sheet to write into
     */
    private void writeHeaderRow(Workbook workbook, Sheet sheet) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(bold);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * Writes one attendee.
     *
     * <p>Nulls are written as empty cells rather than the string "null", which is
     * what {@code setCellValue} on a null-valued getter would otherwise produce for
     * every member who has not linked an account.</p>
     *
     * @param row row to fill
     * @param attendee the attendee
     */
    private void writeAttendeeRow(Row row, EventAttendeeDto attendee) {
        int column = 0;
        writeText(row, column++, attendee.userId() != null ? String.valueOf(attendee.userId()) : null);
        writeText(row, column++, attendee.name());
        writeText(row, column++, attendee.email());
        writeText(row, column++, attendee.phoneNumber());
        writeText(row, column++, attendee.clubRole() != null ? attendee.clubRole().name() : null);
        writeText(row, column++, attendee.avatarUrl());
        writeText(row, column++, attendee.codeforcesHandle());
        writeNumber(row, column++, attendee.cfRating());
        writeText(row, column++, attendee.leetcodeHandle());
        writeNumber(row, column++, attendee.leetcodeRating());
        writeText(row, column++, attendee.codechefUrl());
        writeText(row, column++, attendee.atcoderUrl());
        writeText(row, column++, attendee.githubUrl());
        writeText(row, column++, attendee.linkedinUrl());
        writeText(row, column, attendee.addedAt() != null
                ? attendee.addedAt().format(ADDED_AT_FORMAT)
                : null);
    }

    /**
     * Writes a string, leaving the cell blank when the value is absent.
     *
     * @param row row to write into
     * @param column zero-based column index
     * @param value value, possibly null
     */
    private void writeText(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    /**
     * Writes a number as a number, so the sheet can sort and total it.
     *
     * @param row row to write into
     * @param column zero-based column index
     * @param value value, possibly null
     */
    private void writeNumber(Row row, int column, Integer value) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
    }
}
