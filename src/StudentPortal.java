/* This is the driving engine of the program. It parses the command-line
 * arguments and calls the appropriate methods in the other classes.
 *
 * You should edit this file in two ways:
 * 1) Insert your database username and password in the proper places.
 * 2) Implement the three functions getInformation, registerStudent
 *    and unregisterStudent.
 */
import java.sql.*; // JDBC stuff.
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Scanner;
import java.io.*;  // Reading user input.

public class StudentPortal
{
    /* TODO Here you should put your database name, username and password */
    static final String DATABASE = "jdbc:postgresql://ate.ita.chalmers.se/";
    static final String USERNAME = "tda357_028";
    static final String PASSWORD = "u5ZiHkPv";

    /* Print command usage.
     * /!\ you don't need to change this function! */
    public static void usage () {
        System.out.println("Usage:");
        System.out.println("    i[nformation]");
        System.out.println("    r[egister] <course>");
        System.out.println("    u[nregister] <course>");
        System.out.println("    q[uit]");
    }

    /* main: parses the input commands.
     * /!\ You don't need to change this function! */
    public static void main(String[] args) throws Exception
    {
        try {
            Class.forName("org.postgresql.Driver");
            String url = DATABASE;
            Properties props = new Properties();
            props.setProperty("user",USERNAME);
            props.setProperty("password",PASSWORD);
            Connection conn = DriverManager.getConnection(url, props);

            String student = args[0]; // This is the identifier for the student.

            Console console = System.console();
            // In Eclipse. System.console() returns null due to a bug (https://bugs.eclipse.org/bugs/show_bug.cgi?id=122429)
            // In that case, use the following line instead:
            // BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            usage();
            System.out.println("Welcome!");
            while(true) {
                System.out.print("? > ");
                String mode = console.readLine();
                String[] cmd = mode.split(" +");
                cmd[0] = cmd[0].toLowerCase();
                if ("information".startsWith(cmd[0]) && cmd.length == 1) {
                    /* Information mode */
                    getInformation(conn, student);
                } else if ("register".startsWith(cmd[0]) && cmd.length == 2) {
                    /* Register student mode */
                    registerStudent(conn, student, cmd[1]);
                } else if ("unregister".startsWith(cmd[0]) && cmd.length == 2) {
                    /* Unregister student mode */
                    unregisterStudent(conn, student, cmd[1]);
                } else if ("quit".startsWith(cmd[0])) {
                    break;
                } else usage();
            }
            System.out.println("Goodbye!");
            conn.close();
        } catch (SQLException e) {
            System.err.println(e);
            System.exit(2);
        }
    }

    /* Given a student identification number, ths function should print
     * - the name of the student, the students national identification number
     *   and their issued login name (something similar to a CID)
     * - the programme and branch (if any) that the student is following.
     * - the courses that the student has read, along with the grade.
     * - the courses that the student is registered to. (queue position if the student is waiting for the course)
     * - the number of mandatory courses that the student has yet to read.
     * - whether or not the student fulfills the requirements for graduation
     */
    static void getInformation(Connection conn, String student) throws SQLException
    {
        PreparedStatement pstmt;
        System.out.println("Creating statement...");

        String ssn =null;
        String name=null;
        String login=null;

        String program=null;
        String branch=null;

        LinkedList<String> regCourses = new LinkedList<String>();
        LinkedList<String> waitingCourses = new LinkedList<String>();
        LinkedList<String> gradeCourses = new LinkedList<String>();
        int noOfMandCoursesLeft = 100;
        boolean canGrad=false;

        System.out.println("1");

        pstmt = conn.prepareStatement("SELECT * FROM Student WHERE ssn = ? ");
        ResultSet rs;
        pstmt.setString(1,student);
        rs = pstmt.executeQuery();
        while(rs.next()) {

            //Retrieve by column name
            ssn = rs.getString("ssn");
            name = rs.getString("name");
            login = rs.getString("login");
        }
        rs.close();
        pstmt.close();

        PreparedStatement pstmt2 = conn.prepareStatement("SELECT * FROM BelongsTo WHERE student = ? ");

        pstmt2.setString(1,student);
        rs = pstmt2.executeQuery();
            while(rs.next()) {
                //Retrieve by column name
                branch = rs.getString("branch");
                program = rs.getString("program");
            }

        rs.close();
        pstmt2.close();

        PreparedStatement pstmt3 = conn.prepareStatement("SELECT * FROM Registrations WHERE student = ? AND status = ? ");

        String statusString = "registered";
        pstmt3.setString(1,student);
        pstmt3.setString(2,statusString);
        rs = pstmt3.executeQuery();
        while(rs.next()) {
            //Retrieve by column name
            regCourses.add(rs.getString("course"));
        }

        rs.close();
        pstmt3.close();

        Iterator registered =regCourses.iterator();

        PreparedStatement pstmt4 = conn.prepareStatement("SELECT * FROM CourseQueuePositions WHERE student = ? ");

        pstmt4.setString(1,student);
        rs = pstmt4.executeQuery();
        while(rs.next()) {
            //Retrieve by column name
            waitingCourses.add(rs.getString("course") + " Position:" + rs.getString("place"));
        }

        rs.close();
        pstmt4.close();

        Iterator waitingIt =waitingCourses.iterator();

        PreparedStatement pstmt5= conn.prepareStatement("SELECT * FROM FinishedCourses WHERE student = ? ");

        pstmt5.setString(1,student);
        rs = pstmt5.executeQuery();
        while(rs.next()) {
            //Retrieve by column name
           gradeCourses.add(rs.getString("course") + " Grade:" + rs.getString("grade"));
        }

        rs.close();
        pstmt5.close();

        Iterator gradeIt =gradeCourses.iterator();

        PreparedStatement pstmt6= conn.prepareStatement("SELECT * FROM PathToGraduation WHERE student = ? ");

        pstmt6.setString(1,student);
        rs = pstmt6.executeQuery();
        while(rs.next()) {
            //Retrieve by column name
            noOfMandCoursesLeft = rs.getInt("mandatoryleft");
            canGrad=rs.getBoolean("status");
        }

        rs.close();
        pstmt6.close();

        //Display values
        System.out.println("SSN: " + ssn);
        System.out.println("Name: " + name);
        System.out.println("Login: " + login);
        System.out.println("Branch: " + branch);
        System.out.println("Program: " + program);
        System.out.println("Registered Courses: ");
        while(registered.hasNext()){

            System.out.println("--" +regCourses.poll());
        }

        System.out.println("Waiting Courses: ");
        while(waitingIt.hasNext()){

            System.out.println("----" +waitingCourses.poll());
        }
        System.out.println("Taken Courses: ");
        while(gradeIt.hasNext()){

            System.out.println("------" +gradeCourses.poll());
        }

        System.out.println("MandatoryCourses left: " + noOfMandCoursesLeft);
        System.out.println("Can graduate: " + canGrad);

    }

    /* Register: Given a student id number and a course code, this function
     * should try to register the student for that course.
     */
    static void registerStudent(Connection conn, String student, String course)
            throws SQLException
    {
        PreparedStatement pstmt;
        pstmt = conn.prepareStatement("INSERT INTO Registrations VALUES (?,?)");
        pstmt.setString(1,student);
        pstmt.setString(2,course);
        pstmt.execute();
        System.out.println(student + "was registered to the course " + course);
        pstmt.close();
    }

    /* Unregister: Given a student id number and a course code, this function
     * should unregister the student from that course.
     */
    static void unregisterStudent(Connection conn, String student, String course)
            throws SQLException
    {
        PreparedStatement pstmt;
        pstmt = conn.prepareStatement("DELETE FROM Registrations WHERE student = ? AND course = ?");
        pstmt.setString(1,student);
        pstmt.setString(2,course);
        pstmt.execute();

        System.out.println(student + "was deleted from the course " + course);
        pstmt.close();
    }

}