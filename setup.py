# -*- coding: utf-8 -*-
from setuptools import setup

packages = \
['libvcell', 'libvcell._internal']

package_data = \
{'': ['*']}

install_requires = \
['pydantic>=2.10.6,<3.0.0']

setup_kwargs = {
    'name': 'libvcell',
    'version': '0.0.1',
    'description': 'This is a python package which wraps a subset of VCell Java code as a native python package.',
    'long_description': '# libvcell\n\n[![Release](https://img.shields.io/github/v/release/virtualcell/libvcell)](https://img.shields.io/github/v/release/virtualcell/libvcell)\n[![Build status](https://img.shields.io/github/actions/workflow/status/virtualcell/libvcell/main.yml?branch=main)](https://github.com/virtualcell/libvcell/actions/workflows/main.yml?query=branch%3Amain)\n[![codecov](https://codecov.io/gh/virtualcell/libvcell/branch/main/graph/badge.svg)](https://codecov.io/gh/virtualcell/libvcell)\n[![Commit activity](https://img.shields.io/github/commit-activity/m/virtualcell/libvcell)](https://img.shields.io/github/commit-activity/m/virtualcell/libvcell)\n[![License](https://img.shields.io/github/license/virtualcell/libvcell)](https://img.shields.io/github/license/virtualcell/libvcell)\n\nlibvcell is a subset of VCell algorithms intended to support the [pyvcell](https://pypi.org/project/pyvcell/) python package. libvcell is available as a native executable and a python package.\n\n- **Github repository**: <https://github.com/virtualcell/libvcell/>\n- **Documentation** <https://virtualcell.github.io/libvcell/>\n\n## Getting started with your project\n\nFirst, create a repository on GitHub with the same name as this project, and then run the following commands:\n\n```bash\ngit init -b main\ngit add .\ngit commit -m "init commit"\ngit remote add origin git@github.com:virtualcell/libvcell.git\ngit push -u origin main\n```\n\nFinally, install the environment and the pre-commit hooks with\n\n```bash\nmake install\n```\n\nYou are now ready to start development on your project!\nThe CI/CD pipeline will be triggered when you open a pull request, merge to main, or when you create a new release.\n\nTo finalize the set-up for publishing to PyPI or Artifactory, see [here](https://fpgmaas.github.io/cookiecutter-poetry/features/publishing/#set-up-for-pypi).\nFor activating the automatic documentation with MkDocs, see [here](https://fpgmaas.github.io/cookiecutter-poetry/features/mkdocs/#enabling-the-documentation-on-github).\nTo enable the code coverage reports, see [here](https://fpgmaas.github.io/cookiecutter-poetry/features/codecov/).\n\n## Releasing a new version\n\n- Create an API Token on [PyPI](https://pypi.org/).\n- Add the API Token to your projects secrets with the name `PYPI_TOKEN` by visiting [this page](https://github.com/virtualcell/libvcell/settings/secrets/actions/new).\n- Create a [new release](https://github.com/virtualcell/libvcell/releases/new) on Github.\n- Create a new tag in the form `*.*.*`.\n- For more details, see [here](https://fpgmaas.github.io/cookiecutter-poetry/features/cicd/#how-to-trigger-a-release).\n\n---\n\nRepository initiated with [fpgmaas/cookiecutter-poetry](https://github.com/fpgmaas/cookiecutter-poetry).\n',
    'author': 'Jim Schaff',
    'author_email': 'schaff@uchc.edu',
    'maintainer': 'None',
    'maintainer_email': 'None',
    'url': 'https://github.com/virtualcell/libvcell',
    'packages': packages,
    'package_data': package_data,
    'install_requires': install_requires,
    'python_requires': '>=3.9,<4.0',
}
from build import *
build(setup_kwargs)

setup(**setup_kwargs)
