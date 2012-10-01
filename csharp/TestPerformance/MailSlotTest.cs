using System;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using Microsoft.Win32.SafeHandles;

namespace Statsd
{
    public class MailSlotTest
    {
        public static void TestMailSlot()
        {
            //int i = 0;
            MailSlotTransport transport = new MailSlotTransport();
            //StringBuilder sb = new StringBuilder();
            //while (true)
            //{
            //    if (transport.Ready)
            //    {
            //        sb.Append("|" + sb.Length);
            //        bool retval = transport.Send("csharp.test.mailslot:1|c|" + sb.ToString());
            //        Console.WriteLine("return " + retval);
            //    }
            //}
            string SLOTNAME = @"\\.\mailslot\afcollectorapi";
            SafeFileHandle slotHandle = NativeMailSlot.CreateFile(SLOTNAME,
                            (uint)FileAccess.Write,
                            (uint)FileShare.ReadWrite,
                            0,
                            (uint)FileMode.Open,
                            (uint)FileAttributes.Normal,
                            0);
            FileStream fs = new FileStream(slotHandle, FileAccess.Write);

            UnicodeEncoding encoding = new UnicodeEncoding();
            string data_string = string.Format("{0}:{1}:{2}", Process.GetCurrentProcess().Id, 3, "csharp.test.mailslot:1|c|");
            byte[] data_bytes = encoding.GetBytes(data_string);
            int byteCount = encoding.GetByteCount(data_string);
            fs.Write(data_bytes, 0, byteCount);
            fs.Flush();

            int size = 0, nextsize = 0, count = 0, timeout = 0;
            bool succeeded = false;
            if (slotHandle.IsInvalid)
            {
                Console.WriteLine("MailSlot handle is invalid");
                return;
            }
            try
            {
                succeeded = NativeMailSlot.GetMailslotInfo(slotHandle, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero, IntPtr.Zero);
            }
            catch (Exception e)
            {
                Console.WriteLine(e.StackTrace);
            }
            if (succeeded)
            {
                Console.WriteLine(String.Format("size {0}, nextsize {1}, count {2}, timeout {3}",
                    size, nextsize, count, timeout));
            }
            else
            {
                Console.WriteLine(String.Format("can't get mailslot info {0:d}", Marshal.GetLastWin32Error()));
            }
        }
    }
}
