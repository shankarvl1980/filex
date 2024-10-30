package svl.kadatha.filex;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootUtils {

    public static String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader is = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader es = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            os.writeBytes(command + "\n");
            os.flush();
            os.writeBytes("exit\n");
            os.flush();

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = is.readLine()) != null) {
                output.append(line).append("\n");
            }

            StringBuilder error = new StringBuilder();
            while ((line = es.readLine()) != null) {
                error.append(line).append("\n");
            }

            process.waitFor();
            if (process.exitValue() == 0) {
                return output.toString();
            } else {
                // Log error if needed
                return null;
            }

        } catch (Exception e) {
            // Log exception if needed
            return null;
        }
    }

    public static boolean executeCommandBoolean(String command) {
        String output = executeCommand(command);
        return output != null;
    }

    public static boolean canRunRootCommands() {
        String output = executeCommand("id");
        return output != null && output.contains("uid=0");
    }

    public static String[] listFilesInDirectory(String parentPath) {
        // Construct the command
        String command = "find '" + parentPath + "' -mindepth 1 -maxdepth 1 -print0";

        // Execute the command
        String output = RootUtils.executeCommand(command);

        if (output == null || output.isEmpty()) {
            // Handle error or empty directory
            return null;
        }

        // Split the output using the null character as the delimiter

        // Return the array of file paths
        return output.split("\0");
    }
}
