from distutils.core import setup

setup(
    name='AFStatsd',
    version='0.1.3',
    author='Mike Okner',
    author_email='michael@appfirst.com',
    packages=['afstatsd', 'afstatsd.test'],
    url='http://pypi.python.org/pypi/AFStatsd/',
    description='Statsd Library for use with the AppFirst collector',
    long_description=open('README.txt').read(),
)
