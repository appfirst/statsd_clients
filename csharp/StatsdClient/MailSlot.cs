using System;
using System.Diagnostics;
using System.IO;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using Microsoft.Win32.SafeHandles;

namespace Statsd
{
    public static class NativeMailSlot
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

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)] 
        [return: MarshalAs(UnmanagedType.Bool)]
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

        private static int MAX_MESSAGE_SIZE = 2048;

        private static FileStream mailSlot = null;

        private void ensureMailSlot()
        {
            if (mailSlot == null)
            {
                mailSlot = CreateFileStream(SLOTNAME);
            }
        }

        private FileStream CreateFileStream(String slotName)
        {
            SafeFileHandle slotHandle = NativeMailSlot.CreateFile(
                slotName,
                (uint)FileAccess.Write,
                (uint)FileShare.ReadWrite,
                0,
                (uint)FileMode.Open,
                (uint)FileAttributes.Normal,
                0);
            if (!slotHandle.IsInvalid)
            {
                FileStream fs = new FileStream(slotHandle, FileAccess.Write);
                return fs;
            }
            else
            {
                throw new Exception("MailSlot Cannot be initialized: Handle is Invalid");
            }
        }

        public bool Ready
        {
            get
            {
                lock (mailSlot)
                {
                    return mailSlot != null;
                }
            }
        }

        public bool Send(string mail)
        {
            try
            {
                UnicodeEncoding encoding = new UnicodeEncoding();
                string data_string = string.Format("{0}:{1}:{2}", Process.GetCurrentProcess().Id, 3, mail);
                byte[] data_bytes = encoding.GetBytes(data_string);
                int byteCount = data_bytes.Length;
                if (byteCount > MAX_MESSAGE_SIZE)
                {
                    Console.WriteLine(String.Format(
                        "message size {0} bytes but is limited to {1} bytes, will be truncated",
                        byteCount, MAX_MESSAGE_SIZE));
                    byteCount = MAX_MESSAGE_SIZE;
                }
                ensureMailSlot();
                mailSlot.Write(data_bytes, 0, byteCount);
                mailSlot.Flush();

                Console.WriteLine("sending " + data_string.Substring(0, encoding.GetCharCount(data_bytes, 0, byteCount)));
            }
            catch (IOException ioe)
            {
                this.Close();
                Console.WriteLine(String.Format("{0} Exception caught.", ioe));
                return false;
            }

            return true;
        }

        #region IDisposable Members

        public void Close()
        {
            lock (mailSlot)
            {
                if (mailSlot != null)
                {
                    try
                    {
                        mailSlot.Close();
                    }
                    catch (IOException) { }
                    finally
                    {
                        mailSlot = null;
                    }
                }
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
                return false;
                //retval = this.udpClient.Send(message);
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
