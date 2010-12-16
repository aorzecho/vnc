#!/usr/bin/python
import os

from werkzeug import run_simple
from werkzeug import Response
from werkzeug import Request
from werkzeug.exceptions import BadRequest

ROOT_PATH = os.path.abspath(os.path.dirname(__file__))

@Request.application
def app(request):
    if request.path == '/':
        return Response(file(os.path.join(ROOT_PATH, "index.html")).read(),content_type="text/html")
    else:
        return BadRequest()

if __name__ == '__main__':
    from werkzeug import SharedDataMiddleware
    app = SharedDataMiddleware(app, {
            '/': ROOT_PATH,
    })
    run_simple('localhost', 5000, app,use_debugger=False,use_reloader=True,threaded=False, processes=1)


