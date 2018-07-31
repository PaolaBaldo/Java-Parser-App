package com.ef;

import com.ef.repository.ParserSQLAccess;


/** Assumes UTF-8 encoding. JDK 7+. */
public class Parser {

    public static void main(String... aArgs) throws Exception  {

        String accessLog="C:/log_test/access.log";
        String startDate="2017-01-01.00:00:00";
        String duration="daily";
        String threshold="500";

        if(!aArgs.equals(null) && aArgs.length >= 4){
            accessLog= aArgs[0];
            startDate=aArgs[1];
            duration=aArgs[2];
            threshold=aArgs[3];
        }

        ParserSQLAccess dao = new ParserSQLAccess();
        dao.loadLogFile(accessLog, startDate, duration, threshold);

    }

}
