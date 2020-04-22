import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Thread.sleep;

public class getoriginruntime {

    static final String tablenameDir = "../resource/jobtablename";
    static final String queryDir = "../resource/jobquery";

    public static void main(String[] args) throws InterruptedException, IOException {
        SimpleDateFormat df = new SimpleDateFormat("MM-dd HH:mm:ss");
        File fileDirectory = new File(tablenameDir);
        String[] fileList = fileDirectory.list();
        System.out.println(fileList.length);

        for (int i = 0; i < fileList.length; i++) {
            String queryName = fileList[i];

            StringBuilder sb = new StringBuilder();
            try {
                FileReader queryfr = new FileReader(queryDir + "/" + queryName + ".sql");
                BufferedReader querybr = new BufferedReader(queryfr);
                String queryline;
                while ((queryline = querybr.readLine()) != null) {
                    sb.append(queryline + "\n");
                }
                querybr.close();
                queryfr.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String queryContact = sb.toString();
            //System.out.print(hint);
            String hint = "explain analyze ";

            try {
                sleep(3000);
                Process process = Runtime.getRuntime().exec("./dropCache.sh");
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Connection connection = JDBCUtil.getConnection();
                Statement statement = connection.createStatement();

                String sql = hint + queryContact;
                statement.setQueryTimeout(600000000);
                ResultSet rs = null;
                try {
                    rs = statement.executeQuery(sql);
                    rs.next();
                    //System.out.println(rs.getString(1));
                    //double runtime = Double.valueOf(rs.getString(1).split("=")[4].split("[..]")[0]);
                    //double runtime = Double.valueOf(rs.getString(1).split("=")[1].split("[..]")[0]);
                    ArrayList<String> queryplan = new ArrayList<>();
                    while (rs.next()) {
                        queryplan.add(rs.getString(1));
                    }
                    String realOrder = plan2hint(queryplan, 0, queryplan.size());
                    String planTime = queryplan.get(queryplan.size() - 2).split(" ")[2];
                    String runtime = queryplan.get(queryplan.size() - 1).split(" ")[2];
                    //String output = queryName + "," + runtime;
                    String output = queryName + "," + runtime + "," + planTime + "," + df.format(new Date()) + "," + (realOrder.equals(hintcore));
                    System.out.println(output);

                    rs.close();
                } catch (SQLException e) {
                    String output = queryName + "," + "timeout" + ","  + df.format(new Date());
                    System.out.println(output);
                    continue;
                } finally {
                    statement.close();
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static String plan2hint(ArrayList<String> queryplan, int begin, int end) {
        if (queryplan.get(begin).contains("Scan") && !queryplan.get(begin).contains("Bitmap Index")) {
            String language = queryplan.get(begin);
            String[] word = language.split(" ");
            for (int i = 0; i < word.length; i++) {
                if (word[i].equals("on"))
                    return word[i + 2];
            }
        }
        if (begin == end) {
            return "";
        }
        int blk = blank(queryplan.get(begin));
        int count = 0;
        for (int i = begin + 1; i < end; i++) {
            if (blank(queryplan.get(i)) == blk + 6) {
                count += 1;
                if (count == 2) {
                    String a = plan2hint(queryplan, begin + 1, i);
                    String b = plan2hint(queryplan, i, end);
                    return "( " + a + " " + b + " )";
                }
            }
        }
        return plan2hint(queryplan, begin + 1, end);
    }

    public static int blank(String line) {
        for (int i = 0; i < line.length(); i++)
            if (line.charAt(i) == '-')
                return i;
        return -1;
    }

    public static BufferedWriter path2bw(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) {
            File dir = new File(f.getParent());
            dir.mkdir();
            f.createNewFile();
        }
        FileWriter fw = new FileWriter(f);
        BufferedWriter bw = new BufferedWriter(fw);
        return bw;
    }
}
