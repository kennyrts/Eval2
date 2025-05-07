from setuptools import setup, find_packages

with open("requirements.txt", "r") as f:
    install_requires = f.read().strip().split("\n")

# get version from __version__ variable in custom_tools/__init__.py
from custom_tools import __version__ as version

setup(
    name="custom_tools",
    version=version,
    description="Custom Tools for ERPNext",
    author="Your Company",
    author_email="your@example.com",
    packages=find_packages(),
    zip_safe=False,
    include_package_data=True,
    install_requires=install_requires
) 