/**
 * Flexmojos is a set of maven goals to allow maven users to compile, optimize and test Flex SWF, Flex SWC, Air SWF and Air SWC.
 * Copyright (C) 2008-2012  Marvin Froeder <marvin@flexmojos.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sonatype.flexmojos.test.launcher;

import static org.sonatype.flexmojos.util.CollectionUtils.*;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.StreamPumper;
import org.sonatype.flexmojos.test.AbstractControlledThread;
import org.sonatype.flexmojos.test.ControlledThread;
import org.sonatype.flexmojos.test.TestRequest;
import org.sonatype.flexmojos.test.ThreadStatus;
import org.sonatype.flexmojos.util.OSUtils;
import org.sonatype.flexmojos.util.PathUtil;

/**
 * ActionScript runtime launcher. This class is used to launch the application that runs unit tests.
 */
@Component( role = AsVmLauncher.class, instantiationStrategy = "per-lookup" )
public class AsVmLauncher
    extends AbstractControlledThread
    implements ControlledThread
{

    private class ConsoleConsumer
        implements StreamConsumer
    {

        private String prefix;

        public ConsoleConsumer( String prefix )
        {
            this.prefix = prefix;
        }

        public void consumeLine( String line )
        {
            if ( "\n".equals( line ) )
            {
                return;
            }

            getLogger().debug( prefix + line );
            consoleLog.append( prefix ).append( line ).append( '\n' );
        }

    }

    private boolean allowHeadlessMode;

    private String[] asvmCommand;

    private StringBuffer consoleLog = new StringBuffer();

    private File log;

    private Process process;

    public String getConsoleOutput()
    {
        return this.consoleLog.toString();
    }

    private void processExitCode( int returnCode )
    {

        String errorMessage;

        switch ( returnCode )
        {
            case 0:
                getLogger().debug( "[LAUNCHER] Flashplayer exit as expected" );

                status = ThreadStatus.DONE;
                return;
            case 2:
                if ( useXvfb() )
                {
                    errorMessage = "Xvfb-run error: No command run was specified.";
                    break;
                }
            case 3:
                if ( useXvfb() )
                {
                    errorMessage = "Xvfb-run error: The xauth command is not available.";
                    break;
                }
            case 4:
                if ( useXvfb() )
                {
                    errorMessage =
                        "Xvfb-run error: Temporary directory already exists. This may indicate a race condition.";
                    break;
                }
            case 5:
                if ( useXvfb() )
                {
                    errorMessage =
                        "Xvfb-run error: A problem was encountered while cleanning up the temporary directory.";
                    break;
                }
            case 6:
                if ( useXvfb() )
                {
                    errorMessage = "Xvfb-run error: A problem was encountered while parsing command-line arguments.";
                    break;
                }
            case 139:
                if ( OSUtils.isLinux() )
                {
                    getLogger().debug( "[LAUNCHER] Flashplayer exit as expected" );

                    status = ThreadStatus.DONE;
                    return;
                }
            default:
                errorMessage = "Unexpected return code " + returnCode;
        }

        getLogger().debug( "[LAUNCHER] " + errorMessage );

        status = ThreadStatus.ERROR;
        error = new Error( errorMessage );
    }

    @Override
    protected void reset()
    {
        super.reset();

        process = null;
        consoleLog = new StringBuffer();
    }

    public void run()
    {
        status = ThreadStatus.RUNNING;

        getLogger().debug( "[LAUNCHER] Output pumpers ON" );

        try
        {
            getLogger().debug( "[LAUNCHER] Waiting for flashplayer termination" );
            int returnCode = process.waitFor();
            getLogger().debug( "[LAUNCHER] Flashplayer closed" );

            processExitCode( returnCode );
        }
        catch ( InterruptedException e )
        {
            getLogger().debug( "[LAUNCHER] Process run error: " + e.getMessage() );

            status = ThreadStatus.ERROR;
            error = new Error( "Error while executing external command, process killed.", e );
        }
    }

    private void runFlashplayer( String asvmCommand[], String targetFile )
        throws LaunchFlashPlayerException
    {
        getLogger().warn( "[LAUNCHER] Using regular flashplayer tests" );
        try
        {
            process = Runtime.getRuntime().exec( merge( asvmCommand, new String[] { targetFile } ) );
            new StreamPumper( process.getInputStream(), new ConsoleConsumer( "[SYSOUT]: " ) ).start();
            new StreamPumper( process.getErrorStream(), new ConsoleConsumer( "[SYSERR]: " ) ).start();
        }
        catch ( IOException e )
        {
            throw new LaunchFlashPlayerException( "Failed to launch Flash Player.", e );
        }
    }

    private void runFlashplayerHeadless( String[] asvmCommand, String targetFile )
        throws LaunchFlashPlayerException
    {
        getLogger().warn( "[LAUNCHER] Using xvfb-run to launch headless tests" );

        try
        {
            log = File.createTempFile( "xvfbrun", "flashplayer" );
        }
        catch ( IOException e )
        {
            throw new LaunchFlashPlayerException( "Failed to create xvfb-run error-file!", e );
        }

        try
        {
            process =
                Runtime.getRuntime().exec( merge( new String[] { "xvfb-run", "-a", "-e", log.getAbsolutePath() },
                                                  asvmCommand, new String[] { targetFile } ) );
        }
        catch ( IOException e )
        {
            throw new LaunchFlashPlayerException( "Failed to launch Flash Player in headless environment.", e );
        }
    }

    /**
     * Run the SWF that contains the FlexUnit tests.
     * 
     * @param request the TestRequest instance.
     * @throws LaunchFlashPlayerException Thrown if the flash player fails to launch.
     * @throws InvalidSwfException Thrown if the requested test swf cannot be found or is not set.
     */
    public void start( TestRequest request )
        throws LaunchFlashPlayerException, InvalidSwfException
    {
        reset();

        if ( request == null )
        {
            throw new InvalidSwfException( "request is null" );
        }

        File targetFile;
        if ( request.getUseAirDebugLauncher() )
        {
            targetFile = request.getSwfDescriptor();

            asvmCommand = request.getAdlCommand();

            if ( asvmCommand == null )
            {
                asvmCommand = OSUtils.getPlatformDefaultAdl();
            }
        }
        else
        {
            targetFile = request.getSwf();

            asvmCommand = request.getFlashplayerCommand();

            if ( asvmCommand == null )
            {
                asvmCommand = OSUtils.getPlatformDefaultFlashPlayer();
            }
        }

        allowHeadlessMode = request.getAllowHeadlessMode();

        if ( targetFile == null )
        {
            throw new InvalidSwfException( "targetSwf is null" );
        }

        if ( !targetFile.exists() )
        {
            throw new InvalidSwfException( "targetSwf not found " + targetFile );
        }

        getLogger().debug( "[LAUNCHER] ASVmLauncher starting" );
        getLogger().debug( "[LAUNCHER] exec: " + Arrays.toString( asvmCommand ) + " - " + targetFile );
        getLogger().debug( "[LAUNCHER] Creating process" );

        String target = PathUtil.path( targetFile );
        if ( useXvfb() )
        {
            runFlashplayerHeadless( asvmCommand, target );
        }
        else
        {
            runFlashplayer( asvmCommand, target );
        }

        // kill when VM exits
        Runtime.getRuntime().addShutdownHook( new FlashPlayerShutdownHook( process ) );

        getLogger().debug( "[LAUNCHER] Process created " + process );

        status = ThreadStatus.STARTED;

        launch();
    }

    public void stop()
    {

        if ( process != null )
        {
            try
            {
                process.exitValue();
            }
            catch ( IllegalThreadStateException ex )
            {
                getLogger().debug( "[LAUNCHER] process has not been finished, destroying" );
                process.destroy();
            }
        }

        if ( useXvfb() )
        {
            try
            {
                getLogger().debug( "[LAUNCHER] killing Xvfb" );
                Runtime.getRuntime().exec( new String[] { "killall", "Xvfb" } ).waitFor();
                Runtime.getRuntime().exec( new String[] { "killall", "xvfb-run" } ).waitFor();
                Runtime.getRuntime().exec( new String[] { "killall", new File( asvmCommand[0] ).getName() } ).waitFor();
            }
            catch ( IOException e )
            {
                getLogger().error( "Error killing Xvfb", e );
            }
            catch ( InterruptedException e )
            {
                // ignore, process wake up call
            }

            if ( log != null && log.exists() )
            {
                try
                {
                    consoleLog.append( FileUtils.fileRead( log ) );
                }
                catch ( IOException e )
                {
                    getLogger().error( "Error reading Xvfb log", e );
                }

                log.delete();
            }

        }

    }

    protected boolean useXvfb()
    {
        return allowHeadlessMode && OSUtils.isLinux() && GraphicsEnvironment.isHeadless();
    }

}
