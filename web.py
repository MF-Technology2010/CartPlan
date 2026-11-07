from flask import Flask, render_template, request, redirect
import sqlite3

app = Flask(__name__)

def get_db():
    conn = sqlite3.connect('/opt/grocery-app/grocery.db')
    conn.row_factory = sqlite3.Row
    return conn

@app.route('/')
def index():
    shop_filter = request.args.get('shop', '')
    sort_by = request.args.get('sort', '')
    conn = get_db()
    active = conn.execute('SELECT id FROM shopping_lists WHERE is_active = 1').fetchone()
    active_id = active['id'] if active else 1
    all_shops = conn.execute('SELECT * FROM shops ORDER BY id').fetchall()
    all_cats = conn.execute('SELECT * FROM categories ORDER BY id').fetchall()
    all_lists = conn.execute('SELECT * FROM shopping_lists ORDER BY id').fetchall()
    if shop_filter:
        items = conn.execute('SELECT * FROM items WHERE list_id = ? AND shop = ?', (active_id, shop_filter)).fetchall()
    else:
        items = conn.execute('SELECT * FROM items WHERE list_id = ?', (active_id,)).fetchall()
    items = list(items)
    if sort_by == 'cost_desc':
        items.sort(key=lambda x: x['planned_cost'], reverse=True)
    elif sort_by == 'cost_asc':
        items.sort(key=lambda x: x['planned_cost'])
    budget_row = conn.execute('SELECT budget FROM shopping_lists WHERE id = ?', (active_id,)).fetchone()
    budget = float(budget_row['budget']) if budget_row and budget_row['budget'] else 0
    spent = sum(item['actual_cost'] for item in items if item['is_active'] == 1 and item['is_purchased'])
    planned_active = sum(item['planned_cost'] for item in items if item['is_active'] == 1)
    remaining = budget - spent
    conn.close()
    return render_template('index.html', items=items, budget=budget, remaining=remaining, planned_active=planned_active, shops=all_shops, categories_list=all_cats, shop_filter=shop_filter, sort_by=sort_by,all_lists=all_lists)


@app.route('/add', methods=['POST'])
def add():
    name = request.form['name']
    price = float(request.form['price'])
    quantity = float(request.form['quantity'])
    shop = request.form.get('shop', 'Не выбран')
    category = request.form.get('category', 'Без категории')
    planned = price * quantity
    conn = get_db()
    active = conn.execute('SELECT id FROM shopping_lists WHERE is_active = 1').fetchone()
    active_id = active['id'] if active else 1
    conn.execute('INSERT INTO items (list_id, name, price, quantity, planned_cost, is_active, shop, category) VALUES (?, ?, ?, ?, ?, 1, ?, ?)',
             (active_id, name, price, quantity, planned, shop, category))
    conn.commit()
    conn.close()
    return redirect('/')

@app.route('/purchase/<int:item_id>', methods=['POST'])
def purchase(item_id):
    if request.is_json:
        data = request.get_json()
        actual = float(data.get('actual_cost', 0))
        conn = get_db()
        conn.execute('UPDATE items SET actual_cost = ?, is_purchased = 1 WHERE id = ?', (actual, item_id))
        conn.commit()
        conn.close()
        return {"status": "ok"}
    else:
        actual = float(request.form.get('actual_cost', 0))
        conn = get_db()
        conn.execute('UPDATE items SET actual_cost = ?, is_purchased = 1 WHERE id = ?', (actual, item_id))
        conn.commit()
        conn.close()
        return redirect('/')


@app.route('/deactivate/<int:item_id>', methods=['POST'])
def deactivate(item_id):
    conn = get_db()
    conn.execute('UPDATE items SET is_active = 0 WHERE id = ?', (item_id,))
    conn.commit()
    conn.close()
    return {"status": "ok"}

@app.route('/activate/<int:item_id>', methods=['POST'])
def activate(item_id):
    conn = get_db()
    conn.execute('UPDATE items SET is_active = 1 WHERE id = ?', (item_id,))
    conn.commit()
    conn.close()
    return {"status": "ok"}

@app.route('/delete/<int:item_id>', methods=['POST'])
def delete(item_id):
    conn = get_db()
    conn.execute('DELETE FROM items WHERE id = ?', (item_id,))
    conn.commit()
    conn.close()
    return {"status": "ok"}

@app.route('/categories')
def categories():
    conn = get_db()
    all_cats = conn.execute('SELECT * FROM categories ORDER BY id').fetchall()
    conn.close()
    return render_template('categories.html', categories=all_cats)

@app.route('/categories/add', methods=['POST'])
def categories_add():
    name = request.form['name']
    conn = get_db()
    try:
        conn.execute('INSERT INTO categories (name) VALUES (?)', (name,))
        conn.commit()
    except:
        pass
    conn.close()
    return redirect('/categories')

@app.route('/categories/delete_by_name', methods=['POST'])
def categories_delete_by_name():
    name = request.form.get('name', '')
    conn = get_db()
    cat = conn.execute('SELECT is_system FROM categories WHERE name = ?', (name,)).fetchone()
    if cat and cat['is_system'] == 1:
        conn.close()
        return {'status': 'error', 'message': 'Системная категория — нельзя удалить'}
    conn.execute('DELETE FROM categories WHERE name = ?', (name,))
    conn.commit()
    conn.close()
    return {'status': 'ok'}

@app.route('/categories/delete/<int:cat_id>', methods=['POST'])
def categories_delete(cat_id):
    conn = get_db()
    conn.execute('DELETE FROM categories WHERE id = ?', (cat_id,))
    conn.commit()
    conn.close()
    return redirect('/categories')


@app.route('/shops/list')
def shops_list():
    conn = get_db()
    all_shops = conn.execute('SELECT name FROM shops ORDER BY id').fetchall()
    conn.close()
    return [row['name'] for row in all_shops]

@app.route('/categories/list')
def categories_list():
    conn = get_db()
    all_cats = conn.execute('SELECT id, name, is_system FROM categories ORDER BY is_system DESC, id').fetchall()
    conn.close()
    return [dict(row) for row in all_cats]

@app.route('/set_budget', methods=['POST'])
def set_budget():
    new_budget = request.form.get('budget', '5000')
    conn = get_db()
    active = conn.execute('SELECT id FROM shopping_lists WHERE is_active = 1').fetchone()
    active_id = active['id'] if active else 1
    conn.execute('UPDATE shopping_lists SET budget = ? WHERE id = ?', (new_budget, active_id))
    conn.commit()
    conn.close()
    return redirect('/')

@app.route('/create_list', methods=['POST'])
def create_list():
    name = request.form.get('name', 'Новый список')
    conn = get_db()
    conn.execute('UPDATE shopping_lists SET is_active = 0')
    conn.execute('INSERT INTO shopping_lists (name, budget, is_active) VALUES (?, 0, 1)', (name,))
    conn.commit()
    conn.close()
    return redirect('/')

@app.route('/set_active_list/<int:list_id>')
def set_active_list(list_id):
    conn = get_db()
    conn.execute('UPDATE shopping_lists SET is_active = 0')
    conn.execute('UPDATE shopping_lists SET is_active = 1 WHERE id = ?', (list_id,))
    conn.commit()
    conn.close()
    return redirect('/')


@app.route('/lists')
def lists():
    conn = get_db()
    all_lists = conn.execute('SELECT * FROM shopping_lists ORDER BY id').fetchall()
    conn.close()
    return [dict(row) for row in all_lists]

@app.route('/lists/create', methods=['POST'])
def lists_create():
    name = request.form.get('name', 'Новый список')
    conn = get_db()
    conn.execute('UPDATE shopping_lists SET is_active = 0')
    conn.execute('INSERT INTO shopping_lists (name, budget, is_active) VALUES (?, 0, 1)', (name,))
    conn.commit()
    conn.close()
    return {"status": "ok"}

@app.route('/lists/rename', methods=['POST'])
def lists_rename():
    list_id = request.form.get('id', '')
    new_name = request.form.get('name', '')
    if list_id and new_name:
        conn = get_db()
        conn.execute('UPDATE shopping_lists SET name = ? WHERE id = ?', (new_name, list_id))
        conn.commit()
        conn.close()
    return {"status": "ok"}

@app.route('/lists/set_active/<int:list_id>', methods=['POST'])
def lists_set_active(list_id):
    conn = get_db()
    conn.execute('UPDATE shopping_lists SET is_active = 0')
    conn.execute('UPDATE shopping_lists SET is_active = 1 WHERE id = ?', (list_id,))
    conn.commit()
    conn.close()
    return {"status": "ok"}

@app.route('/lists/delete/<int:list_id>', methods=['POST'])
def lists_delete(list_id):
    conn = get_db()
    conn.execute('DELETE FROM items WHERE list_id = ?', (list_id,))
    conn.execute('DELETE FROM shopping_lists WHERE id = ?', (list_id,))
    conn.commit()
    conn.close()
    return {"status": "ok"}


@app.route('/budget')
def get_budget():
    conn = get_db()
    active = conn.execute('SELECT id FROM shopping_lists WHERE is_active = 1').fetchone()
    active_id = active['id'] if active else 1
    items = conn.execute('SELECT * FROM items WHERE list_id = ?', (active_id,)).fetchall()

    active_id = active['id'] if active else 1

    items = conn.execute('SELECT * FROM items WHERE list_id = ?', (active_id,)).fetchall()

    budget_row = conn.execute('SELECT budget FROM shopping_lists WHERE id = ?', (active_id,)).fetchone()
    budget = float(budget_row['budget']) if budget_row and budget_row['budget'] else 0
    planned = sum(item['planned_cost'] for item in items if item['is_active'] == 1)
    spent = sum(item['actual_cost'] for item in items if item['is_active'] == 1 and item['is_purchased'])
    remaining = budget - spent
    conn.close()
    return {"budget": budget, "planned": planned, "spent": spent, "remaining": remaining}

@app.route('/items')
def get_items_active():
    shop_filter = request.args.get('shop', '')
    cat_filter = request.args.get('category', '')
    sort_by = request.args.get('sort', '')
    conn = get_db()
    active = conn.execute('SELECT id FROM shopping_lists WHERE is_active = 1').fetchone()
    active_id = active['id'] if active else 1
    query = 'SELECT * FROM items WHERE list_id = ?'
    params = [active_id]
    if shop_filter:
        query += ' AND shop = ?'
        params.append(shop_filter)
    if cat_filter:
        query += ' AND category = ?'
        params.append(cat_filter)
    items = conn.execute(query, params).fetchall()
    items = [dict(item) for item in items]
    if sort_by == 'cost_desc':
        items.sort(key=lambda x: x['planned_cost'], reverse=True)
    elif sort_by == 'cost_asc':
        items.sort(key=lambda x: x['planned_cost'])
    conn.close()
    return items

@app.route('/lists/<int:list_id>/items')
def get_items(list_id):
    shop_filter = request.args.get('shop', '')
    cat_filter = request.args.get('category', '')
    sort_by = request.args.get('sort', '')
    conn = get_db()
    query = 'SELECT * FROM items WHERE list_id = ?'
    params = [list_id]
    if shop_filter:
        query += ' AND shop = ?'
        params.append(shop_filter)
    if cat_filter:
        query += ' AND category = ?'
        params.append(cat_filter)
    items = conn.execute(query, params).fetchall()
    items = [dict(item) for item in items]
    if sort_by == 'cost_desc':
        items.sort(key=lambda x: x['planned_cost'], reverse=True)
    elif sort_by == 'cost_asc':
        items.sort(key=lambda x: x['planned_cost'])
    conn.close()
    return items

@app.route('/shops')
def shops():
    conn = get_db()
    all_shops = conn.execute('SELECT * FROM shops ORDER BY id').fetchall()
    conn.close()
    return render_template('shops.html', shops=all_shops)

@app.route('/shops/add', methods=['POST'])
def shops_add():
    name = request.form['name']
    conn = get_db()
    try:
        conn.execute('INSERT INTO shops (name) VALUES (?)', (name,))
        conn.commit()
    except:
        pass
    conn.close()
    return redirect('/shops')

@app.route('/shops/delete/<int:shop_id>', methods=['POST'])
def shops_delete(shop_id):
    conn = get_db()
    conn.execute('DELETE FROM shops WHERE id = ?', (shop_id,))
    conn.commit()
    conn.close()
    return redirect('/shops')

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5005)
