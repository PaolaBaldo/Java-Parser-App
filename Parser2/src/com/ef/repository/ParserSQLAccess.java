package com.ef.repository;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;


public class ParserSQLAccess {
    private Connection connect = null;
    private Statement statement = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    public void loadLogFile(String accessLog, String startDate, String duration, String threshold) throws Exception {
        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connect = DriverManager
                    .getConnection("jdbc:mysql://localhost/parser?"
                            + "user=sqluser&password=sqluserpw&serverTimezone=UTC&useLegacyDatetimeCode=false");

            try {
                statement = connect.createStatement();
                statement.executeUpdate( "LOAD DATA INFILE '"+accessLog+"' INTO TABLE logs FIELDS TERMINATED BY '|' " +
                        "LINES TERMINATED BY '\r\n' (DATE, IP, REQUEST, STATUS, USERAGENT);");


            }catch(SQLException e) {
                    System.out.println("Exception during 'access.log' file loading: "+ e);
            }
            verifyBlockedIPs( startDate,  duration,  threshold);
        } catch (Exception e) {
            throw e;
        } finally {
            close();
        }

    }

    public void verifyBlockedIPs(String startDate, String duration, String threshold) throws Exception{
        try {
            preparedStatement = connect
                    .prepareStatement("SELECT" +
                            "    ip, COUNT(*)" +
                            " FROM" +
                            "    parser.logs" +
                            " WHERE  date BETWEEN '" + startDate + "' AND ? " +
                            " GROUP BY" +
                            "    ip" +
                            " HAVING " +
                            "    COUNT(*) > "+threshold+";");

            DateFormat formatter;
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            startDate= startDate.replace(".", " ");
            Date date = formatter.parse(startDate);
            java.sql.Timestamp endDate = new Timestamp(date.getTime());

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(endDate.getTime());

            if(duration.equals("hourly")){
                cal.add(Calendar.HOUR, 1);
            }else{
                if(duration.equals("daily")){
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
            endDate = new Timestamp(cal.getTime().getTime());
            preparedStatement.setTimestamp(1, endDate);
            resultSet = preparedStatement.executeQuery();
            saveBlockedIP(resultSet, threshold);

        } catch(SQLException e) {
            System.out.println("Exception: "+ e);
        }finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void saveBlockedIP(ResultSet resultSet, String threshold) throws SQLException {

        while (resultSet.next()) {

            String ip = resultSet.getString("ip");
            String count = resultSet.getString("COUNT(*)");
            System.out.println("IP: " + ip);
            System.out.println("Count: " + count);

            preparedStatement = connect
                    .prepareStatement("insert into  parser.blocked_ip values (default, ?, ?)");

            preparedStatement.setString(1, ip);
            preparedStatement.setString(2, "IP has more the "+threshold+" requests.");

            preparedStatement.executeUpdate();

        }
    }

    // You need to close the resultSet
    private void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }

            if (statement != null) {
                statement.close();
            }

            if (connect != null) {
                connect.close();
            }
        } catch (Exception e) {

        }
    }

}

