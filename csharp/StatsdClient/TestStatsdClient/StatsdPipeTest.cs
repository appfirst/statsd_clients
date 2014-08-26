using Statsd;
using Microsoft.VisualStudio.TestTools.UnitTesting;
namespace TestStatsdClient
{
    
    
    /// <summary>
    ///This is a test class for StatsdPipeTest and is intended
    ///to contain all StatsdPipeTest Unit Tests
    ///</summary>
    [TestClass()]
    public class StatsdPipeTest
    {


        private TestContext testContextInstance;

        /// <summary>
        ///Gets or sets the test context which provides
        ///information about and functionality for the current test run.
        ///</summary>
        public TestContext TestContext
        {
            get
            {
                return testContextInstance;
            }
            set
            {
                testContextInstance = value;
            }
        }

        #region Additional test attributes
        // 
        //You can use the following additional attributes as you write your tests:
        //
        //Use ClassInitialize to run code before running the first test in the class
        //[ClassInitialize()]
        //public static void MyClassInitialize(TestContext testContext)
        //{
        //}
        //
        //Use ClassCleanup to run code after all tests in a class have run
        //[ClassCleanup()]
        //public static void MyClassCleanup()
        //{
        //}
        //
        //Use TestInitialize to run code before running each test
        //[TestInitialize()]
        //public void MyTestInitialize()
        //{
        //}
        //
        //Use TestCleanup to run code after each test has run
        //[TestCleanup()]
        //public void MyTestCleanup()
        //{
        //}
        //
        #endregion


        /// <summary>
        ///A test for UpdateCount
        ///</summary>
        [TestMethod()]
        public void UpdateCountTest3()
        {
            StatsdPipe target = new StatsdPipe(); // TODO: Initialize to an appropriate value
            string message = string.Empty; // TODO: Initialize to an appropriate value
            int magnitude = 0; // TODO: Initialize to an appropriate value
            string[] keys = null; // TODO: Initialize to an appropriate value
            bool expected = false; // TODO: Initialize to an appropriate value
            bool actual;
            actual = target.UpdateCount(magnitude, keys);
            Assert.AreEqual(expected, actual);
            Assert.Inconclusive("Verify the correctness of this test method.");
        }

        /// <summary>
        ///A test for UpdateCount
        ///</summary>
        [TestMethod()]
        public void UpdateCountTest2()
        {
            StatsdPipe target = new StatsdPipe(); // TODO: Initialize to an appropriate value
            int magnitude = 0; // TODO: Initialize to an appropriate value
            double sampleRate = 0F; // TODO: Initialize to an appropriate value
            string[] keys = null; // TODO: Initialize to an appropriate value
            bool expected = false; // TODO: Initialize to an appropriate value
            bool actual;
            actual = target.UpdateCount(magnitude, sampleRate, keys);
            Assert.AreEqual(expected, actual);
            Assert.Inconclusive("Verify the correctness of this test method.");
        }

        /// <summary>
        ///A test for UpdateCount
        ///</summary>
        [TestMethod()]
        public void UpdateCountTest1()
        {
            StatsdPipe target = new StatsdPipe(); // TODO: Initialize to an appropriate value
            int magnitude = 0; // TODO: Initialize to an appropriate value
            string[] keys = null; // TODO: Initialize to an appropriate value
            bool expected = false; // TODO: Initialize to an appropriate value
            bool actual;
            actual = target.UpdateCount(magnitude, keys);
            Assert.AreEqual(expected, actual);
            Assert.Inconclusive("Verify the correctness of this test method.");
        }

        /// <summary>
        ///A test for Timing
        ///</summary>
        [TestMethod()]
        public void TimingTest1()
        {
            StatsdPipe target = new StatsdPipe(); // TODO: Initialize to an appropriate value
            string key = string.Empty; // TODO: Initialize to an appropriate value
            int value = 0; // TODO: Initialize to an appropriate value
            bool expected = false; // TODO: Initialize to an appropriate value
            bool actual;
            actual = target.Timing(key, value);
            Assert.AreEqual(expected, actual);
            Assert.Inconclusive("Verify the correctness of this test method.");
        }

        /// <summary>
        ///A test for Increment
        ///</summary>
        [TestMethod()]
        public void IncrementTest()
        {
            StatsdPipe target = new StatsdPipe(); // TODO: Initialize to an appropriate value
            string[] keys = null; // TODO: Initialize to an appropriate value
            bool expected = false; // TODO: Initialize to an appropriate value
            bool actual;
            actual = target.Increment(keys);
            Assert.AreEqual(expected, actual);
            Assert.Inconclusive("Verify the correctness of this test method.");
        }

        /// <summary>
        ///A test for Gauge
        ///</summary>
        [TestMethod()]
        public void GaugeTest()
        {
            StatsdPipe target = new StatsdPipe(); // TODO: Initialize to an appropriate value
            string key = string.Empty; // TODO: Initialize to an appropriate value
            int value = 0; // TODO: Initialize to an appropriate value
            bool expected = false; // TODO: Initialize to an appropriate value
            bool actual;
            actual = target.Gauge(key, value);
            Assert.AreEqual(expected, actual);
            Assert.Inconclusive("Verify the correctness of this test method.");
        }

        /// <summary>
        ///A test for Decrement
        ///</summary>
        [TestMethod()]
        public void DecrementTest()
        {
            StatsdPipe target = new StatsdPipe(); // TODO: Initialize to an appropriate value
            string[] keys = null; // TODO: Initialize to an appropriate value
            bool expected = false; // TODO: Initialize to an appropriate value
            bool actual;
            actual = target.Decrement(keys);
            Assert.AreEqual(expected, actual);
            Assert.Inconclusive("Verify the correctness of this test method.");
        }
    }
}
