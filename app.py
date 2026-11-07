from flask import Flask, jsonify
import sqlite3

app = Flask(__name__)

@app.route('/')
def home():
    return jsonify({"status": "Grocery App is running"})

@app.route('/lists')
def get_lists():
    conn = sqlite3.connect('/opt/grocery-app/grocery.db')
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()
    cur.execute("SELECT * FROM shopping_lists")
    rows = cur.fetchall()
    conn.close()
    return jsonify([dict(row) for row in rows])
@app.route('/add_item', methods=['POST'])
def add_item():
    return jsonify({"status": "будет реализовано позже"})
@app.route('/lists/<int:list_id>/items')
def get_items(list_id):
    conn = sqlite3.connect('/opt/grocery-app/grocery.db')
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()
    cur.execute("SELECT * FROM items WHERE list_id = ?", (list_id,))
    rows = cur.fetchall()
    conn.close()
    return jsonify([dict(row) for row in rows])
from flask import request

@app.route('/items/<int:item_id>/purchase', methods=['POST'])
def purchase_item(item_id):
    data = request.get_json()
    actual_cost = data.get('actual_cost', 0)
    
    conn = sqlite3.connect('/opt/grocery-app/grocery.db')
    cur = conn.cursor()
    cur.execute(
        "UPDATE items SET actual_cost = ?, is_purchased = 1 WHERE id = ?",
        (actual_cost, item_id)
    )
    conn.commit()
    conn.close()
    return jsonify({"status": "ok"})
if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5004)
