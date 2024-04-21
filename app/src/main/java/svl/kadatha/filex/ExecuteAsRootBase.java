package svl.kadatha.filex;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import timber.log.Timber;

public abstract class ExecuteAsRootBase
{
    public static boolean canRunRootCommands()
    {
        boolean retval = false;
        Process suProcess;

        try
        {
            suProcess = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            //DataInputStream osRes = new DataInputStream(suProcess.getInputStream());
            BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(suProcess.getInputStream()));

            if (null != os && null != bufferedReader)
            {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n");
                os.flush();

                String currUid = bufferedReader.readLine();
                boolean exitSu = false;
                if (null == currUid)
                {
                    retval = false;
                    exitSu = false;
                    //Timber.tag("ROOT").d( "Can't get root access or denied by user");
                }
                else if (currUid.contains("uid=0"))
                {
                    retval = true;
                    exitSu = true;
                    //Timber.tag("ROOT").d( "Root access granted");
                }
                else
                {
                    retval = false;
                    exitSu = true;
                    //Timber.tag("ROOT").d( "Root access rejected: " + currUid);
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
            //Timber.tag("ROOT").d( "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;
    }

    public final boolean execute()
    {
        boolean retval = false;

        try
        {
            ArrayList<String> commands = getCommandsToExecute();
            if (null != commands && !commands.isEmpty())
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
                    // Root access granted
                    // Root access denied
                    retval = 255 != suProcessRetval;
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
    protected abstract ArrayList<String> getCommandsToExecute();
}
