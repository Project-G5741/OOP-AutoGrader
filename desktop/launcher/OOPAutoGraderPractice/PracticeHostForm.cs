using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace OopAutoGrader.Practice;

internal sealed class PracticeHostForm : Form
{
    private readonly WebView2 _webView = new() { Dock = DockStyle.Fill };
    private readonly Label _status = new()
    {
        Dock = DockStyle.Top,
        Height = 36,
        TextAlign = ContentAlignment.MiddleCenter,
        Text = "Starting OOP AutoGrader practice…",
    };

    public PracticeHostForm()
    {
        Text = "OOP AutoGrader — Practice";
        Width = 1280;
        Height = 800;
        StartPosition = FormStartPosition.CenterScreen;
        Controls.Add(_webView);
        Controls.Add(_status);
        FormClosing += (_, e) =>
        {
            if (e.CloseReason == CloseReason.UserClosing)
            {
                BackendLauncher.Stop();
            }
        };
        Shown += async (_, _) => await InitializeAsync();
    }

    private async Task InitializeAsync()
    {
        try
        {
            await _webView.EnsureCoreWebView2Async();
            _webView.CoreWebView2.Settings.AreDevToolsEnabled = false;
            _webView.Source = new Uri(BackendLauncher.AppUrl);
            _status.Visible = false;
        }
        catch (Exception ex)
        {
            MessageBox.Show(this, ex.Message, "Practice launcher", MessageBoxButtons.OK, MessageBoxIcon.Error);
            Close();
        }
    }
}
