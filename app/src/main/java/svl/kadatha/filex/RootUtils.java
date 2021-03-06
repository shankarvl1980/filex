package svl.kadatha.filex;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class RootUtils {

    public static boolean EXECUTE(List<String> commands)
    {
        boolean retval = false;

        try
        {
            if (null != commands && commands.size() > 0)
            {
                Process suProcess = Runtime.getRuntime().exec("su");

                DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());

                // Execute commands that require root access
                for (String currCommand : commands)
                {
                    os.writeBytes(currCommand + "\n");
                    os.flush();
                }

                os.writeBytes("exit\n");
                os.flush();

                try
                {
                    int suProcessRetval = suProcess.waitFor();
                    if (255 != suProcessRetval)
                    {
                        // Root access granted
                        retval = true;
                        MainActivity.SU="su";
                    }
                    else
                    {
                        // Root access denied
                        retval = false;
                        MainActivity.SU="";
                        //add listener to disable su toggle
                    }
                }
                catch (Exception ex)
                {
                    Log.e("ROOT", "Error executing root action", ex);
                }
            }
        }
        catch (IOException | SecurityException ex)
        {
            Log.w("ROOT", "Can't get root access", ex);
        } catch (Exception ex)
        {
            Log.w("ROOT", "Error executing internal operation", ex);
        }

        return retval;
    }


    public static boolean CAN_RUN_ROOT_COMMANDS()
    {
        return false;
        /*
        boolean retval = false;
        Process suProcess;

        try
        {
            suProcess = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

            if (null != os && null != osRes)
            {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n");
                os.flush();

                String currUid = osRes.readLine();
                boolean exitSu = false;
                if (null == currUid)
                {
                    retval = false;
                    exitSu = false;
                    Log.d("ROOT", "Can't get root access or denied by user");
                    MainActivity.SU="";
                }
                else if (currUid.contains("uid=0"))
                {
                    retval = true;
                    exitSu = true;
                    Log.d("ROOT", "Root access granted");
                    MainActivity.SU="su";
                }
                else
                {
                    retval = false;
                    exitSu = true;
                    Log.d("ROOT", "Root access rejected: " + currUid);
                    MainActivity.SU="";
                    //add listener to disable su toggle
                }

                if (exitSu)
                {
                    os.writeBytes("exit\n");
                    os.flush();
                }
            }
        }
        catch (Exception e)
        {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted

            retval = false;
            Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;

         */
    }

    public static boolean WHETHER_FILE_EXISTS(String file_path)
    {
        return false;
    }

}

