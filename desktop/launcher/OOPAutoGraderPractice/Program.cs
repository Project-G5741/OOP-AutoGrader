namespace OopAutoGrader.Practice;



internal static class Program

{

    [STAThread]

    private static void Main()

    {

        Application.SetUnhandledExceptionMode(UnhandledExceptionMode.CatchException);

        Application.ThreadException += (_, e) => ShowFatal(e.Exception);

        AppDomain.CurrentDomain.UnhandledException += (_, e) => ShowFatal(e.ExceptionObject as Exception);



        ApplicationConfiguration.Initialize();

        var installDir = AppContext.BaseDirectory.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);

        try

        {

            BackendLauncher.Start(installDir);

            using var cts = new CancellationTokenSource(TimeSpan.FromMinutes(2));

            BackendLauncher.WaitUntilHealthyAsync(cts.Token).GetAwaiter().GetResult();

            Application.Run(new PracticeHostForm());

        }

        catch (Exception ex)

        {

            ShowFatal(ex);

        }

        finally

        {

            BackendLauncher.Stop();

        }

    }



    private static void ShowFatal(Exception? ex)

    {

        if (ex == null)

        {

            return;

        }

        Log(installDir: AppContext.BaseDirectory, ex);

        var hint = ex is FileNotFoundException or DirectoryNotFoundException

            ? $"{ex.Message}{Environment.NewLine}{Environment.NewLine}Try OOP-AutoGrader-Practice.bat to see Java errors."

            : ex.Message;

        MessageBox.Show(hint, "OOP AutoGrader Practice", MessageBoxButtons.OK, MessageBoxIcon.Error);

    }



    private static void Log(string installDir, Exception ex)

    {

        try

        {

            var logPath = Path.Combine(installDir, "launcher-error.log");

            File.AppendAllText(logPath, $"[{DateTime.Now:O}] {ex}{Environment.NewLine}");

        }

        catch

        {

            // ignore logging failures

        }

    }

}

