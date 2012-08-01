using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Runtime.InteropServices;
using Microsoft.Win32.SafeHandles;

namespace MailSlotClient
{
    public static class External
    {
        [DllImport("kernel32.dll", CharSet = CharSet.Auto, CallingConvention = CallingConvention.StdCall, SetLastError = true)]
        public static extern SafeFileHandle CreateFile(
              string lpFileName,
              uint dwDesiredAccess,
              uint dwShareMode,
              uint SecurityAttributes,
              uint dwCreationDisposition,
              uint dwFlagsAndAttributes,
              int hTemplateFile
              );

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, CallingConvention = CallingConvention.StdCall, SetLastError = true)]
        public static extern SafeFileHandle CreateMailslot(string lpName, uint nMaxMessageSize,
           uint lReadTimeout, IntPtr lpSecurityAttributes);

        [DllImport("kernel32.dll")]
        public static extern bool GetMailslotInfo(SafeFileHandle hMailslot, IntPtr lpMaxMessageSize,
           IntPtr lpNextSize, IntPtr lpMessageCount, IntPtr lpReadTimeout);

        [DllImport("kernel32.dll")]
        public static extern bool WriteFile(SafeFileHandle hFile, byte[] lpBuffer,
           uint nNumberOfBytesToWrite, out uint lpNumberOfBytesWritten,
           [In] ref System.Threading.NativeOverlapped lpOverlapped);

        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        public static extern bool CloseHandle(SafeFileHandle hObject);

        [DllImport("kernel32.dll")]
        public static extern bool ReadFile(IntPtr hFile, byte[] lpBuffer,
           uint nNumberOfBytesToRead, out uint lpNumberOfBytesRead, IntPtr lpOverlapped);
    }

}
