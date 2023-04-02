package svl.kadatha.filex;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import timber.log.Timber;

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
                    Timber.tag("ROOT").e( "Error executing root action");
                }
            }
        }
        catch (IOException | SecurityException ex)
        {
            Timber.tag("ROOT").w( "Can't get root access");
        } catch (Exception ex)
        {
            Timber.tag("ROOT").w( "Error executing internal operation");
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
                    Timber.tag("ROOT").d( "Can't get root access or denied by user");
                    MainActivity.SU="";
                }
                else if (currUid.contains("uid=0"))
                {
                    retval = true;
                    exitSu = true;
                    Timber.tag("ROOT").d( "Root access granted");
                    MainActivity.SU="su";
                }
                else
                {
                    retval = false;
                    exitSu = true;
                    Timber.tag("ROOT").d( "Root access rejected: " + currUid);
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
            Timber.tag("ROOT").d( "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;

         */
    }

    public static boolean WHETHER_FILE_EXISTS(String file_path)
    {
        return false;
    }

}

