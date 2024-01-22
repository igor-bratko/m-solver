from flask import Flask, request, send_file
import subprocess
import os

app = Flask(__name__)

@app.route('/convert', methods=['POST'])
def convert():
    # Save the incoming DOT file
    dot_file = request.files['dotfile']
    dot_file_path = '/tmp/graph.dot'
    dot_file.save(dot_file_path)

    # Convert to SVG using Graphviz
    svg_file_path = '/tmp/graph.svg'
    subprocess.run(['dot', '-Tsvg', dot_file_path, '-o', svg_file_path])

    # Return the SVG file
    return send_file(svg_file_path, mimetype='image/svg+xml')

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
