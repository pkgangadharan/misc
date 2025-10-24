import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.translate.v3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class GoogleCloudTranslator {

    /**
     * Main method to demonstrate the three translation types.
     *
     * <p>Update the TODO placeholders with your actual project details.
     */
    public static void main(String[] args)
        throws IOException, ExecutionException, InterruptedException {

        // --- TODO: Update these variables ---
        String projectId = "your-gcp-project-id";
        String location = "global"; // Use "global" for most translation needs
        String sourceLanguage = "en"; // English
        String targetLanguage = "es"; // Spanish

        // GCS paths for batch jobs
        // Example: "gs://your-bucket-name/inputs/"
        String gcsInputPrefix = "gs://your-bucket-name/input-files/";
        // Example: "gs://your-bucket-name/outputs/"
        String gcsOutputPrefix = "gs://your-bucket-name/translated-output/";
        // ------------------------------------

        // --- 1. Simple Text Translation ---
        System.out.println("--- Starting Simple Text Translation ---");
        List<String> smallTexts = List.of("Hello world", "How are you?", "This is a test.");
        try {
            List<String> translatedSmallTexts =
                translateTextList(
                    projectId, location, smallTexts, targetLanguage, sourceLanguage);
            System.out.println("Original: " + smallTexts);
            System.out.println("Translated: " + translatedSmallTexts);
        } catch (Exception e) {
            System.err.println("Failed to translate simple text: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("----------------------------------------\n");

        // --- 2. Batch Large Text Translation ---
        // Assumes you have text files in GCS
        // e.g., gs://your-bucket-name/input-files/large-text-1.txt
        //       gs://your-bucket-name/input-files/large-text-2.txt
        System.out.println("--- Starting Batch Large Text Translation ---");
        List<String> gcsTextFiles =
            List.of(gcsInputPrefix + "large-text-1.txt", gcsInputPrefix + "large-text-2.txt");
        try {
            batchTranslateLargeText(
                projectId,
                location,
                gcsTextFiles,
                gcsOutputPrefix + "text/", // Output will go here
                targetLanguage,
                sourceLanguage);
            System.out.println(
                "Batch text translation job submitted. Results will be in: "
                    + gcsOutputPrefix
                    + "text/");
        } catch (Exception e) {
            System.err.println("Failed to batch translate text: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("-------------------------------------------\n");

        // --- 3. Batch Document Translation ---
        // Assumes you have documents in GCS
        // e.g., gs://your-bucket-name/input-files/document1.pdf
        //       gs://your-bucket-name/input-files/document2.docx
        System.out.println("--- Starting Batch Document Translation ---");
        List<String> gcsDocumentFiles =
            List.of(gcsInputPrefix + "document1.pdf", gcsInputPrefix + "document2.docx");
        try {
            batchTranslateDocuments(
                projectId,
                location,
                gcsDocumentFiles,
                gcsOutputPrefix + "documents/", // Output will go here
                targetLanguage,
                sourceLanguage);
            System.out.println(
                "Batch document translation job submitted. Results will be in: "
                    + gcsOutputPrefix
                    + "documents/");
        } catch (Exception e) {
            System.err.println("Failed to batch translate documents: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("-------------------------------------------\n");
    }

    /**
     * Translates a list of small text strings using the direct TranslateText API.
     *
     * @param projectId Your GCP project ID.
     * @param location The location (e.g., "global").
     * @param texts The list of strings to translate.
     * @param targetLanguage The BCP-47 language code of the target language (e.g., "es").
     * @param sourceLanguage The BCP-47 language code of the source language (e.g., "en").
     * @return A list of translated strings.
     */
    public static List<String> translateTextList(
        String projectId,
        String location,
        List<String> texts,
        String targetLanguage,
        String sourceLanguage)
        throws IOException {

        // Initialize client
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            LocationName parent = LocationName.of(projectId, location);

            // Build the request
            TranslateTextRequest request =
                TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setSourceLanguageCode(sourceLanguage)
                    .setTargetLanguageCode(targetLanguage)
                    .addAllContents(texts)
                    .build();

            // Call the API
            TranslateTextResponse response = client.translateText(request);

            // Extract and return the translated texts
            return response.getTranslationsList().stream()
                .map(Translation::getTranslatedText)
                .collect(Collectors.toList());
        }
    }

    /**
     * Translates large text files stored in GCS using BatchTranslateText.
     *
     * @param projectId Your GCP project ID.
     * @param location The location (e.g., "global").
     * @param gcsInputUris A list of GCS URIs pointing to the input text files (e.g.,
     * "gs://my-bucket/input.txt").
     * @param gcsOutputUriPrefix The GCS URI prefix where translated files will be written (e.g.,
     * "gs://my-bucket/output/").
     * @param targetLanguage The BCP-47 language code of the target language.
     * @param sourceLanguage The BCP-47 language code of the source language.
     */
    public static void batchTranslateLargeText(
        String projectId,
        String location,
        List<String> gcsInputUris,
        String gcsOutputUriPrefix,
        String targetLanguage,
        String sourceLanguage)
        throws IOException, ExecutionException, InterruptedException {

        // Initialize client
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            LocationName parent = LocationName.of(projectId, location);

            // Configure input sources
            List<InputConfig> inputConfigs =
                gcsInputUris.stream()
                    .map(
                        uri ->
                            InputConfig.newBuilder()
                                .setGcsSource(GcsSource.newBuilder().setInputUri(uri).build())
                                .setMimeType("text/plain") // MIME type is required for text
                                .build())
                    .collect(Collectors.toList());

            // Configure output destination
            OutputConfig outputConfig =
                OutputConfig.newBuilder()
                    .setGcsDestination(
                        GcsDestination.newBuilder().setOutputUriPrefix(gcsOutputUriPrefix).build())
                    .build();

            // Build the batch request
            BatchTranslateTextRequest request =
                BatchTranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setSourceLanguageCode(sourceLanguage)
                    .addTargetLanguageCodes(targetLanguage)
                    .addAllInputConfigs(inputConfigs)
                    .setOutputConfig(outputConfig)
                    .build();

            // Submit the asynchronous batch job
            OperationFuture<BatchTranslateResponse, BatchTranslateMetadata> future =
                client.batchTranslateTextAsync(request);

            System.out.println("Batch text translation job submitted. Waiting for completion...");
            // Wait for the operation to complete
            BatchTranslateResponse response = future.get();
            System.out.printf(
                "Batch text translation complete. Total characters: %d\n",
                response.getTotalCharacters());
        }
    }

    /**
     * Translates documents (PDF, DOCX, etc.) stored in GCS using BatchTranslateDocument.
     *
     * @param projectId Your GCP project ID.
     * @param location The location (e.g., "global").
     * @param gcsInputUris A list of GCS URIs pointing to the input documents (e.g.,
     * "gs://my-bucket/doc.pdf").
     * @param gcsOutputUriPrefix The GCS URI prefix where translated documents will be written.
     * @param targetLanguage The BCP-47 language code of the target language.
     * @param sourceLanguage The BCP-47 language code of the source language.
     */
    public static void batchTranslateDocuments(
        String projectId,
        String location,
        List<String> gcsInputUris,
        String gcsOutputUriPrefix,
        String targetLanguage,
        String sourceLanguage)
        throws IOException, ExecutionException, InterruptedException {

        // Initialize client
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            LocationName parent = LocationName.of(projectId, location);

            // Configure input sources
            // MIME type is auto-detected for documents, so it's not set here.
            List<BatchDocumentInputConfig> inputConfigs =
                gcsInputUris.stream()
                    .map(
                        uri ->
                            BatchDocumentInputConfig.newBuilder()
                                .setGcsSource(GcsSource.newBuilder().setInputUri(uri).build())
                                .build())
                    .collect(Collectors.toList());

            // Configure output destination
            BatchDocumentOutputConfig outputConfig =
                BatchDocumentOutputConfig.newBuilder()
                    .setGcsDestination(
                        GcsDestination.newBuilder().setOutputUriPrefix(gcsOutputUriPrefix).build())
                    .build();

            // Build the batch request
            BatchTranslateDocumentRequest request =
                BatchTranslateDocumentRequest.newBuilder()
                    .setParent(parent.toString())
                    .setSourceLanguageCode(sourceLanguage)
                    .addTargetLanguageCodes(targetLanguage) // Can add multiple targets
                    .addAllInputConfigs(inputConfigs)
                    .setOutputConfig(outputConfig)
                    .build();

            // Submit the asynchronous batch job
            OperationFuture<BatchTranslateDocumentResponse, BatchTranslateDocumentMetadata> future =
                client.batchTranslateDocumentAsync(request);

            System.out.println("Batch document translation job submitted. Waiting for completion...");
            // Wait for the operation to complete
            BatchTranslateDocumentResponse response = future.get();
            System.out.printf(
                "Batch document translation complete. Total pages: %d\n", response.getTotalPages());
        }
    }
}
