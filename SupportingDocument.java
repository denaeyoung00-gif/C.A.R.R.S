public class SupportingDocument {
    private final String fileName;
    private final String extension;
    private final long fileSizeBytes;

    public SupportingDocument(String fileName, String extension, long fileSizeBytes) {
        this.fileName = fileName;
        this.extension = extension.toLowerCase().replace(".", "");
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFileName() { return fileName; }
    public String getExtension() { return extension; }
    public long getFileSizeBytes() { return fileSizeBytes; }
}
