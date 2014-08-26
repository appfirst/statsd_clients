from distutils.core import setup

setup(
    name='AFStatsd',
    version='1.1.0',
    description=('Statsd Library for use with the AppFirst collector '
                 '(http://www.appfirst.com)'),
    author='Mike Okner',
    author_email='michael@appfirst.com',
    packages=['afstatsd', 'afstatsd.test'],
    url="https://github.com/appfirst/statsd_clients/tree/master/python",
    long_description=open('README.txt').read(),
)
