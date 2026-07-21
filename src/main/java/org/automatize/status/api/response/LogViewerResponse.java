package org.automatize.status.api.response;

import java.util.List;

/**
 * <p>
 * Response object exposing the contents of a raw log file for the in-app log viewer.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Return a page of log file lines to the log viewer UI</li>
 *   <li>Indicate whether the returned content was truncated for size limits</li>
 *   <li>Convey the underlying file path and total size for context</li>
 * </ul>
 * </p>
 *
 * <p>
 * Licensed under the Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0).
 * You may share and adapt this work for non-commercial purposes, as long as appropriate credit is given.
 * </p>
 *
 * @author Tim De Smedt
 */
public class LogViewerResponse {

    /** The individual lines of the log file. */
    private List<String> lines;

    /** The total number of lines available in the log file. */
    private int totalLines;

    /** Whether the returned content was truncated. */
    private boolean truncated;

    /** The path of the log file being viewed. */
    private String filePath;

    /** The total size of the log file in bytes. */
    private long fileSizeBytes;

    /**
     * Default constructor.
     */
    public LogViewerResponse() {
    }

    /**
     * Gets the log file lines.
     *
     * @return the log file lines
     */
    public List<String> getLines() { return lines; }

    /**
     * Sets the log file lines.
     *
     * @param lines the log file lines to set
     */
    public void setLines(List<String> lines) { this.lines = lines; }

    /**
     * Gets the total number of lines.
     *
     * @return the total number of lines
     */
    public int getTotalLines() { return totalLines; }

    /**
     * Sets the total number of lines.
     *
     * @param totalLines the total number of lines to set
     */
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }

    /**
     * Gets the truncated flag.
     *
     * @return true if the content was truncated, false otherwise
     */
    public boolean isTruncated() { return truncated; }

    /**
     * Sets the truncated flag.
     *
     * @param truncated the truncated flag to set
     */
    public void setTruncated(boolean truncated) { this.truncated = truncated; }

    /**
     * Gets the log file path.
     *
     * @return the log file path
     */
    public String getFilePath() { return filePath; }

    /**
     * Sets the log file path.
     *
     * @param filePath the log file path to set
     */
    public void setFilePath(String filePath) { this.filePath = filePath; }

    /**
     * Gets the file size in bytes.
     *
     * @return the file size in bytes
     */
    public long getFileSizeBytes() { return fileSizeBytes; }

    /**
     * Sets the file size in bytes.
     *
     * @param fileSizeBytes the file size in bytes to set
     */
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
}
