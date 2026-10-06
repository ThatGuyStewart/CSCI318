
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class QueryH2 {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:h2:file:product-service/data/productdb";
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT PRODUCT_ID, NAME, CATEGORY, PRICE FROM PRODUCT_VIEW ORDER BY PRODUCT_ID");
            while (rs.next()) {
                System.out.println(rs.getLong(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3) + "\t" + rs.getDouble(4));
            }
        }
    }
}
