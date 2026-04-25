import adrianmikula.jakartamigration.pdfreporting.service.impl.HtmlToPdfReportServiceImpl;
import java.lang.reflect.Method;

public class TestDemo {
    public static void main(String[] args) {
        try {
            HtmlToPdfReportServiceImpl service = new HtmlToPdfReportServiceImpl();
            
            Method headerMethod = HtmlToPdfReportServiceImpl.class.getDeclaredMethod("generateSharedHeader", String.class, String.class, String.class);
            headerMethod.setAccessible(true);
            
            String header = (String) headerMethod.invoke(service, "Test Report", "Test Project", "Test Type");
            
            System.out.println("Header generation test: " + (header.contains("report-header") ? "PASS" : "FAIL"));
            System.out.println("Header contains plugin icon: " + (header.contains("plugin-icon") ? "PASS" : "FAIL"));
            System.out.println("Header contains report title: " + (header.contains("Test Report") ? "PASS" : "FAIL"));
            
            Method footerMethod = HtmlToPdfReportServiceImpl.class.getDeclaredMethod("generateSharedFooter", String.class, int.class, int.class);
            footerMethod.setAccessible(true);
            
            String footer = (String) footerMethod.invoke(service, "Test Report", 1, 5);
            System.out.println("Footer generation test: " + (footer.contains("report-footer") ? "PASS" : "FAIL"));
            System.out.println("Footer contains page info: " + (footer.contains("Page 1 of 5") ? "PASS" : "FAIL"));
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
