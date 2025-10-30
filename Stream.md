You can stream a file as a response in Spring Boot using several methods, with the best choice depending on the file size and source.
The most common and flexible method is using ResponseEntity combined with an InputStreamResource. For very large files, StreamingResponseBody is the most memory-efficient option.
1. Using ResponseEntity<InputStreamResource> (Recommended)
This approach is memory-efficient and works for files from any source, including the local filesystem or cloud storage (like AWS S3), as long as you can get an InputStream.
It reads the file in chunks and writes them to the response, rather than loading the entire file into memory.
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RestController
public class FileStreamController {

    @GetMapping("/download-file")
    public ResponseEntity<Resource> downloadFile() throws FileNotFoundException {
        
        // 1. Get your file from a source (e.g., local disk, S3, etc.)
        File file = new File("path/to/your/file.pdf");
        InputStream inputStream = new FileInputStream(file);

        // 2. Create an InputStreamResource
        InputStreamResource resource = new InputStreamResource(inputStream);

        // 3. Set HTTP headers
        HttpHeaders headers = new HttpHeaders();
        
        // This header tells the browser to prompt a download
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=file.pdf");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length()) // Set the file size
                .contentType(MediaType.APPLICATION_OCTET_STREAM) // Set the MIME type
                .body(resource);
    }
}

2. Using StreamingResponseBody (For Very Large Files)
This is the most advanced and memory-efficient method, ideal for streaming extremely large files or dynamically generated content. It writes directly to the response OutputStream in a separate thread, which avoids blocking the main request thread.
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

@RestController
public class FileStreamController {

    @GetMapping("/download-large-file")
    public ResponseEntity<StreamingResponseBody> streamLargeFile() {

        File file = new File("path/to/your/large-file.zip");

        // 1. Create the StreamingResponseBody
        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = new FileInputStream(file)) {
                // Read from input and write to output in chunks
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            // The try-with-resources block automatically closes the InputStream
        };

        // 2. Set HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=large-file.zip");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }
}

Comparison of Methods
| Method | When to Use | Memory Usage |
|---|---|---|
| ResponseEntity<byte[]> | Small files only (< 10-20 MB). | High. Loads the entire file into memory. |
| ResponseEntity<InputStreamResource> | Most common cases. Good for medium-to-large files from any source. | Low. Streams the file in chunks. |
| StreamingResponseBody | Extremely large files or dynamic streams. | Lowest. Asynchronously writes to the output stream. |
💡 Key Considerations
 * Content-Disposition: This header is crucial. Setting it to attachment; filename="your-file.ext" tells the browser to open a "Save As..." dialog instead of trying to display the file inline.
 * Content-Type: Set the correct MIME type (e.g., MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_PNG_VALUE, or MediaType.APPLICATION_OCTET_STREAM for a generic binary file).
 * Streaming from Cloud Storage (S3, etc.): The InputStreamResource method is perfect for this. Your S3 client's "get object" command will return an InputStream (e.g., S3ObjectInputStream). You can wrap this stream directly in an InputStreamResource and return it.
This video guide provides a practical example of implementing streaming in a Spring RESTful service, which can help visualize the code in action.

YouTube video views will be stored in your YouTube History, and your data will be stored and used by YouTube according to its Terms of Service
