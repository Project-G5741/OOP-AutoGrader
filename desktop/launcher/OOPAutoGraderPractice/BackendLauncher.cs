using System.Diagnostics;
using System.Net.Http;
using System.Text.Json;

namespace OopAutoGrader.Practice;

internal static class BackendLauncher
{
    private const int Port = 18002;
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(2) };

    internal static Process? Process { get; private set; }

    internal static string HomeDirectory { get; private set; } = "";

    internal static string AppUrl => $"http://127.0.0.1:{Port}/";

    internal static void Start(string installDir)
    {
        HomeDirectory = installDir;
        var backendJar = Path.Combine(installDir, "backend.jar");
        if (!File.Exists(backendJar))
        {
            throw new FileNotFoundException("backend.jar was not found next to the launcher.", backendJar);
        }

        var javaExe = ResolveJava(installDir);
        var psi = new ProcessStartInfo
        {
            FileName = javaExe,
            Arguments = $"-jar \"{backendJar}\"",
            WorkingDirectory = installDir,
            UseShellExecute = false,
            // Hide the Java console from the desktop and taskbar; students use the WebView window only.
            CreateNoWindow = true,
        };
        psi.Environment["APP_DESKTOP_HOME"] = installDir;
        psi.Environment["SPRING_PROFILES_ACTIVE"] = "desktop";
        psi.Environment["JWT_SECRET"] = "desktop-local-dev-secret-minimum-32-bytes!!";
        // Worker OT must use the same portable JDK as the API on clean machines (no PATH java).
        psi.Environment["DESKTOP_WORKER_JAVA"] = javaExe;

        Process = Process.Start(psi)
                  ?? throw new InvalidOperationException("Failed to start the Java backend process.");
    }

    internal static async Task WaitUntilHealthyAsync(CancellationToken cancellationToken)
    {
        var deadline = DateTime.UtcNow.AddMinutes(2);
        while (DateTime.UtcNow < deadline)
        {
            cancellationToken.ThrowIfCancellationRequested();
            try
            {
                using var response = await Http.GetAsync($"{AppUrl}api/desktop/status", cancellationToken);
                if (response.IsSuccessStatusCode)
                {
                    var json = await response.Content.ReadAsStringAsync(cancellationToken);
                    using var doc = JsonDocument.Parse(json);
                    // Wait until pack bootstrap finished — Tomcat accepts traffic before ApplicationReadyEvent.
                    if (doc.RootElement.TryGetProperty("bootstrapComplete", out var complete)
                        && complete.ValueKind == JsonValueKind.True)
                    {
                        return;
                    }
                }
            }
            catch (HttpRequestException)
            {
                // backend still starting
            }
            catch (TaskCanceledException) when (!cancellationToken.IsCancellationRequested)
            {
                // HttpClient timeout while warming up
            }

            await Task.Delay(250, cancellationToken);
        }

        throw new TimeoutException("The practice backend did not become ready in time.");
    }

    internal static void Stop()
    {
        var process = Process;
        if (process == null)
        {
            return;
        }

        try
        {
            if (!process.HasExited)
            {
                // Windowless java has no main window; CloseMainWindow is a no-op — kill the tree promptly.
                if (!process.CloseMainWindow() || !process.WaitForExit(3000))
                {
                    if (!process.HasExited)
                    {
                        process.Kill(entireProcessTree: true);
                    }
                }
            }
        }
        catch
        {
            // best effort shutdown
        }
        finally
        {
            Process = null;
        }
    }

    private static string ResolveJava(string installDir)
    {
        var bundled = Path.Combine(installDir, "runtime", "jdk", "bin", "java.exe");
        if (File.Exists(bundled))
        {
            return bundled;
        }

        return "java";
    }
}
