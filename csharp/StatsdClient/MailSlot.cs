using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Threading;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using Microsoft.Win32.SafeHandles;
using System.Diagnostics;

namespace Statsd
{
    static class NativeMailSlot
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
           [In] ref NativeOverlapped lpOverlapped);

        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        public static extern bool CloseHandle(SafeFileHandle hObject);

        [DllImport("kernel32.dll")]
        public static extern bool ReadFile(IntPtr hFile, byte[] lpBuffer,
           uint nNumberOfBytesToRead, out uint lpNumberOfBytesRead, IntPtr lpOverlapped);
    }

    public class MailSlotTransport : ITransport, IDisposable
    {
        private static string SLOTNAME = @"\\.\mailslot\afcollectorapi";

        private static SafeFileHandle slotHandle = null;

        private static FileStream fs = null;

        private FileStream mailSlot
        {
            get
            {
                if (fs == null)
                {
                    if (slotHandle == null || slotHandle.IsInvalid || slotHandle.IsClosed)
                    {
                        slotHandle = NativeMailSlot.CreateFile(SLOTNAME,
                            (uint)FileAccess.Write,
                            (uint)FileShare.ReadWrite,
                            0,
                            (uint)FileMode.Open,
                            (uint)FileAttributes.Normal,
                            0);
                    }
                    if (!slotHandle.IsInvalid)
                    {
                        fs = new FileStream(slotHandle, FileAccess.Write);
                    }
                    else
                    {
                        throw new Exception("MailSlot Handle is Invalid");
                    }
                }
                return fs;
            }
        }

        public bool Ready
        {
            get
            {
                return mailSlot != null;
            }
        }

        public bool Send(string mail)
        {
            try
            {
                UnicodeEncoding encoding = new UnicodeEncoding();
                string data_string = string.Format("{0}:{1}:{2}", Process.GetCurrentProcess().Id, 3, mail);
                byte[] data_bytes = encoding.GetBytes(data_string);
                int byteCount = encoding.GetByteCount(data_string);

                mailSlot.Write(data_bytes, 0, byteCount);
                mailSlot.Flush();

                Console.WriteLine("sending " + data_string);
            }
            catch (IOException ioe)
            {
                Debug.WriteLine(String.Format("{0} Exception caught.", ioe));
            }

            return true;
        }

        #region IDisposable Members

        public void Close()
        {
            if (fs != null)
            {
                fs.Close();
                fs = null;
            }
            else if (slotHandle != null && !slotHandle.IsInvalid)
            {
                slotHandle.Close();
            }
        }

        public void Dispose()
        {
            this.Close();
        }

        #endregion
    }

    public class AFTransport : MailSlotTransport
    {
        private readonly UDPTransport udpClient = new UDPTransport();

        public new bool Send(string message)
        {
            bool usingCollector = this.Ready;
            bool retval = false;
            if (usingCollector)
            {
                retval = base.Send(message);
            }
            else
            {
                retval = this.udpClient.Send(message);
            }
            return retval;
        }
    }

    public class UDPTransport : ITransport
    {
        private readonly UdpClient udpClient = new UdpClient();

        public string Hostname = "localhost";

        public int Port = 8125;

        public bool Send(string message)
        {
            var data = Encoding.Default.GetBytes(message + "\n");

            udpClient.Send(data, data.Length, Hostname, Port);
            return true;
        }
    }
}
