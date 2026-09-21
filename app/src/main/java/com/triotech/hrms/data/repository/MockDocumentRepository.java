package com.triotech.hrms.data.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * In-memory mock catalog of the demo employee's documents, ordered by category so
 * the Documents screen can render section headers in a stable order. Files are not
 * stored — {@link com.triotech.hrms.core.util.SampleDocumentGenerator} materialises
 * a placeholder PDF/image on demand when a document is viewed, downloaded or shared.
 */
public class MockDocumentRepository implements DocumentRepository {

    private static final long SIMULATED_LATENCY_MS = 500L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Document> catalog = buildCatalog();

    @NonNull
    @Override
    public LiveData<Resource<List<Document>>> observeDocuments() {
        MutableLiveData<Resource<List<Document>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading());
        mainHandler.postDelayed(() ->
                liveData.setValue(catalog.isEmpty()
                        ? Resource.empty()
                        : Resource.success(new ArrayList<>(catalog))), SIMULATED_LATENCY_MS);
        return liveData;
    }

    @Nullable
    @Override
    public Document findById(@NonNull String id) {
        for (Document d : catalog) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        return null;
    }

    @NonNull
    private static List<Document> buildCatalog() {
        return new ArrayList<>(Arrays.asList(
                doc("payslip-2026-09", Document.Category.PAYSLIPS, "Payslip - September 2026",
                        Document.FileType.PDF, date(2026, 9, 30), 98_446),
                doc("payslip-2026-08", Document.Category.PAYSLIPS, "Payslip - August 2026",
                        Document.FileType.PDF, date(2026, 8, 31), 97_210),
                doc("offer-letter", Document.Category.OFFER_LETTER, "Offer Letter",
                        Document.FileType.PDF, date(2022, 5, 20), 184_320),
                doc("appointment-letter", Document.Category.APPOINTMENT_LETTER, "Appointment Letter",
                        Document.FileType.PDF, date(2022, 6, 13), 176_128),
                doc("experience-letter", Document.Category.EXPERIENCE_LETTER, "Experience Letter",
                        Document.FileType.PDF, date(2025, 4, 1), 152_064),
                doc("form-16-fy2425", Document.Category.TAX_DOCUMENTS, "Form 16 - FY 2024-25",
                        Document.FileType.PDF, date(2025, 6, 15), 210_944),
                doc("investment-proof", Document.Category.TAX_DOCUMENTS, "Investment Proof Acknowledgement",
                        Document.FileType.JPG, date(2025, 1, 10), 512_000),
                doc("code-of-conduct", Document.Category.COMPANY_POLICIES, "Code of Conduct",
                        Document.FileType.PDF, date(2024, 1, 5), 245_760),
                doc("leave-policy", Document.Category.COMPANY_POLICIES, "Leave Policy",
                        Document.FileType.PDF, date(2024, 1, 5), 133_120),
                doc("id-card-scan", Document.Category.OTHER, "ID Card Scan",
                        Document.FileType.PNG, date(2022, 6, 14), 384_000)
        ));
    }

    @NonNull
    private static Document doc(String id, Document.Category category, String name,
            Document.FileType type, long dateMillis, long sizeBytes) {
        return new Document(id, category, name, type, dateMillis, sizeBytes);
    }

    private static long date(int year, int month, int day) {
        Calendar cal = DateUtils.calendarFor(year, month, day);
        return cal.getTimeInMillis();
    }
}
