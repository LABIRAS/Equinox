CREATE FUNCTION TO_DEGREES(ARG1 DOUBLE) RETURNS DOUBLE
PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA
EXTERNAL NAME 'java.lang.Math.toDegrees'