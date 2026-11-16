'use strict';
const $ = id => document.getElementById(id);
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
const fmt = n => new Intl.NumberFormat('ru-RU').format(n);
let state = {positions: [], suppliers: [], customers: [], shipments: []};
let loaded = false, page = 'inventory', grouped = false, modalAction = '', customerFilter = null, saving = false, requestId = 0;
const pages = {
  inventory: ['Товары и остатки','Товары, наличие и движение запасов в одном месте.','Каталог товаров','+ Новый товар','product'],
  suppliers: ['Поставщики','Контакты партнёров и товары, которые они поставляют.','Справочник поставщиков','+ Поставщик','supplier'],
  customers: ['Покупатели','Карточки покупателей и история выданных товаров.','Справочник покупателей','+ Покупатель','customer'],
  history: ['История выдач','Все выдачи товаров покупателям, от последних к первым.','Журнал выдач','↗ Выдать товар','ship']
};
async function api(path, body) {
  let response;
  try { response = await fetch('/api' + path, {method:body ? 'POST' : 'GET', headers:body ? {'Content-Type':'application/json'} : {}, body:body ? JSON.stringify(body) : undefined}); }
  catch { throw new Error('Нет связи с приложением. Проверьте подключение и обновите данные.'); }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Не удалось выполнить запрос. Попробуйте обновить страницу.');
  return data;
}
async function refresh() {
  const id = ++requestId;
  $('refresh').disabled = true;
  try {
    const [positions,suppliers,customers,shipments] = await Promise.all(['/inventory','/suppliers','/customers','/shipments'].map(path => api(path)));
    if (id !== requestId) return false;
    state = {positions,suppliers,customers,shipments}; loaded = true;
    $('load-error').hidden = true; $('connection').textContent = 'Данные актуальны'; $('connection').className = 'connection online';
    const currentType = $('type').value;
    $('type').innerHTML = '<option value="">Все виды</option>' + [...new Set(positions.map(p=>p.product.type))].sort((a,b)=>a.localeCompare(b,'ru')).map(type=>`<option value="${escapeHtml(type)}">${escapeHtml(type)}</option>`).join('');
    $('type').value = currentType;
    const available = positions.filter(p=>p.quantity>0).length;
    $('metric-products').textContent = fmt(positions.length);
    $('metric-stock').textContent = fmt(positions.reduce((sum,p)=>sum+p.quantity,0));
    $('metric-available').textContent = fmt(available); $('metric-empty').textContent = fmt(positions.length-available);
    render(); return true;
  } catch (error) {
    if (id !== requestId) return false;
    $('load-error').hidden = false; $('load-error').querySelector('span').textContent = error.message;
    $('connection').textContent = 'Нет связи'; $('connection').className = 'connection';
    if (!loaded) $('data').innerHTML = empty('Не удалось загрузить склад','Проверьте запуск приложения и базы данных. Затем нажмите «Повторить».');
    return false;
  } finally { if (id === requestId) $('refresh').disabled = false; }
}
function empty(title, description, action, label) {
  return `<div class="empty"><span class="empty-icon">▦</span><h3>${escapeHtml(title)}</h3><p>${escapeHtml(description)}</p>${action ? `<button class="primary" data-action="${action}">${escapeHtml(label)}</button>` : ''}</div>`;
}
function table(headers, rows) { return `<div class="table-scroll"><table><thead><tr>${headers.map(h=>`<th>${h}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table></div>`; }
function supplierName(id) { return state.suppliers.find(s=>s.id===id)?.name || '—'; }
function customerName(id) { return state.customers.find(c=>c.id===id)?.name || '—'; }
function productName(id) { return state.positions.find(p=>p.product.id===id)?.product.name || '—'; }
function productRow(position) {
  const {product:p,quantity:q}=position;
  return `<tr><td><div class="product-cell"><span class="product-symbol">▣</span><div><strong>${escapeHtml(p.name)}</strong><small class="cell-sub">${escapeHtml(supplierName(p.supplierId))}</small></div></div></td><td><span class="type-badge">${escapeHtml(p.type)}</span></td><td><span class="quantity">${fmt(q)}</span> шт.</td><td><span class="status ${q>0?'yes':'no'}">${q>0?'● В наличии':'Нет в наличии'}</span></td><td><div class="row-actions"><button class="row-button" data-action="receive" data-id="${p.id}" aria-label="Принять ${escapeHtml(p.name)}">+ Приём</button><button class="row-button" data-action="ship" data-id="${p.id}" ${q===0?'disabled':''}>Выдать</button></div></td></tr>`;
}
function render() {
  if (!loaded) return;
  const search = $('search').value.trim().toLocaleLowerCase('ru');
  const matches = (...values) => values.some(v=>String(v).toLocaleLowerCase('ru').includes(search));
  let html='',count=0;
  if (page==='inventory') {
    let rows = state.positions.filter(p=>matches(p.product.name,p.product.type,supplierName(p.product.supplierId)) && (!$('type').value || p.product.type===$('type').value) && (!$('available').checked || p.quantity>0));
    count=rows.length;
    if (grouped) rows=[...rows].sort((a,b)=>a.product.type.localeCompare(b.product.type,'ru'));
    let previousType=null; const rendered=[];
    for (const row of rows) { if (grouped && row.product.type!==previousType) { previousType=row.product.type; rendered.push(`<tr class="group-row"><td colspan="5">${escapeHtml(previousType)}</td></tr>`); } rendered.push(productRow(row)); }
    html=rows.length ? table(['ТОВАР / ПОСТАВЩИК','ВИД','ОСТАТОК','СТАТУС','ДЕЙСТВИЯ'],rendered) : state.positions.length ? empty('Ничего не найдено','Попробуйте изменить запрос или сбросить фильтры.') : empty('Ваш склад начинается здесь','Сначала добавьте поставщика, затем создайте товар и оформите поступление.',state.suppliers.length?'product':'supplier',state.suppliers.length?'Создать первый товар':'Добавить поставщика');
  } else if (page==='suppliers') {
    const rows=state.suppliers.filter(s=>matches(s.name,s.contact)); count=rows.length;
    html=rows.length ? table(['ПОСТАВЩИК','КОНТАКТ','ТОВАРЫ'],rows.map(s=>{const products=state.positions.filter(p=>p.product.supplierId===s.id);return `<tr><td><strong>${escapeHtml(s.name)}</strong></td><td>${escapeHtml(s.contact)}</td><td>${products.length ? products.map(p=>escapeHtml(p.product.name)).join(', ') : '<span class="cell-sub">Пока нет товаров</span>'}</td></tr>`;})) : empty('Поставщиков пока нет','Добавьте партнёра, чтобы связать с ним товары.','supplier','Добавить поставщика');
  } else if (page==='customers') {
    const rows=state.customers.filter(c=>matches(c.name,c.contact));count=rows.length;
    html=rows.length ? table(['ПОКУПАТЕЛЬ','КОНТАКТ','ВЫДАНО ЕДИНИЦ','ИСТОРИЯ'],rows.map(c=>`<tr><td><strong>${escapeHtml(c.name)}</strong></td><td>${escapeHtml(c.contact)}</td><td>${fmt(state.shipments.filter(s=>s.customerId===c.id).reduce((sum,s)=>sum+s.quantity,0))} шт.</td><td><button class="row-button" data-history="${c.id}">Посмотреть →</button></td></tr>`)) : empty('Покупателей пока нет','Создайте карточку покупателя, чтобы оформить выдачу.','customer','Добавить покупателя');
  } else {
    const rows=state.shipments.filter(s=>(!customerFilter || s.customerId===customerFilter) && matches(productName(s.productId),customerName(s.customerId))).reverse();count=rows.length;
    html=customerFilter ? `<div class="history-summary">Покупатель: <strong>${escapeHtml(customerName(customerFilter))}</strong> <button class="text-button" data-clear-history>Показать всех</button></div>` : '';
    html+=rows.length ? table(['ПОКУПАТЕЛЬ','ТОВАР','КОЛИЧЕСТВО','ОПЕРАЦИЯ'],rows.map(s=>`<tr><td><strong>${escapeHtml(customerName(s.customerId))}</strong></td><td>${escapeHtml(productName(s.productId))}</td><td class="quantity">${fmt(s.quantity)} шт.</td><td><span class="status yes">Выдано</span></td></tr>`)) : empty('Выдач пока нет','Здесь появятся товары, переданные покупателям.');
  }
  $('data').innerHTML=html;$('count').textContent=fmt(count);$('result-count').textContent=`Показано записей: ${fmt(count)}`;
  document.querySelectorAll('[data-action="receive"]').forEach(b=>b.disabled=!state.positions.length);
  document.querySelectorAll('#stock-actions [data-action="ship"]').forEach(b=>b.disabled=!state.customers.length || !state.positions.some(p=>p.quantity>0));
}
function navigate() {
  page=location.hash.slice(1);if(!pages[page]) page='inventory';
  const [title,description,section,button]=pages[page];
  $('title').textContent=title;$('breadcrumb').textContent=title;$('subtitle').textContent=description;$('section-title').textContent=section;$('create-button').textContent=button;
  document.querySelectorAll('nav a').forEach(a=>{a.classList.toggle('active',a.dataset.page===page);if(a.dataset.page===page)a.setAttribute('aria-current','page');else a.removeAttribute('aria-current');});
  for (const id of ['stock-actions','type-filter','available-filter','group']) $(id).hidden=page!=='inventory';
  $('search').value='';$('search').placeholder=page==='inventory'?'Найти по названию или виду…':'Поиск по имени, названию или контакту…';
  if(page!=='history')customerFilter=null;
  render();
}
function field(name,label,input) { return `<label class="field">${label}${input}</label>`; }
function textInput(name,label,max) { return field(name,label,`<input name="${name}" maxlength="${max}" required autocomplete="off">`); }
function selectInput(name,label,options,selected) { return field(name,label,`<select name="${name}" required><option value="">Выберите…</option>${options.map(o=>`<option value="${o.id}" ${o.id===selected?'selected':''}>${escapeHtml(o.name)}</option>`).join('')}</select>`); }
function openForm(action, selected) {
  if(!loaded) {toast('Сначала дождитесь загрузки данных.');return;}
  if(action==='product'&&!state.suppliers.length){toast('Сначала добавьте поставщика.');openForm('supplier');return;}
  if(['receive','ship'].includes(action)&&!state.positions.length){toast('Сначала создайте товар.');return;}
  if(action==='ship'&&!state.customers.length){toast('Сначала добавьте покупателя.');openForm('customer');return;}
  modalAction=action;let fields='',title='',description='';
  if(action==='supplier'||action==='customer') {
    title=action==='supplier'?'Новый поставщик':'Новый покупатель';description='Укажите имя или название организации и удобный способ связи.';
    fields=textInput('name',action==='supplier'?'Название поставщика':'Имя покупателя',200)+textInput('contact','Телефон, почта или другой контакт',300);
  } else if(action==='product') {
    title='Новый товар';description='Создаётся карточка с нулевым остатком. Товар поступит на склад после приёма.';
    fields=textInput('name','Название товара',200)+textInput('type','Вид товара',100)+selectInput('supplierId','Поставщик',state.suppliers);
  } else {
    title=action==='receive'?'Приём товара':'Выдача товара';description=action==='receive'?'Добавьте поступившее количество к текущему остатку.':'Выберите получателя и количество. Остаток уменьшится после подтверждения.';
    fields=selectInput('productId','Товар',state.positions.map(p=>({id:p.product.id,name:`${p.product.name} · ${p.product.type} · ${p.quantity} шт.`})),selected);
    if(action==='ship')fields+=selectInput('customerId','Покупатель',state.customers);
    fields+=field('quantity','Количество, шт.','<input name="quantity" type="number" min="1" max="2147483647" step="1" required inputmode="numeric" placeholder="Например, 10">');
  }
  $('dialog-title').textContent=title;$('dialog-description').textContent=description;$('fields').innerHTML=fields;$('form-error').hidden=true;$('submit').textContent=['receive','ship'].includes(action)?'Подтвердить':'Создать';
  $('dialog').showModal();
}
let toastTimer;
function toast(message) { clearTimeout(toastTimer);$('toast').textContent=message;$('toast').hidden=false;toastTimer=setTimeout(()=>$('toast').hidden=true,6500); }
$('form').addEventListener('submit',async event=>{
  event.preventDefault();if(saving)return;
  const body=Object.fromEntries(new FormData($('form')));
  if(body.quantity!==undefined)body.quantity=Number(body.quantity);
  const paths={product:'/products',supplier:'/suppliers',customer:'/customers',receive:'/receipts',ship:'/shipments'};
  saving=true;$('submit').disabled=true;$('cancel').disabled=true;$('close-dialog').disabled=true;$('form-error').hidden=true;
  try {
    await api(paths[modalAction],body);
    $('dialog').close();
    const refreshed=await refresh();
    toast(refreshed?'Готово. Изменения сохранены.':'Операция сохранена, но список не обновился. Нажмите «Обновить» перед следующей операцией.');
  } catch(error){$('form-error').textContent=error.message;$('form-error').hidden=false;}
  finally{saving=false;$('submit').disabled=false;$('cancel').disabled=false;$('close-dialog').disabled=false;}
});
document.addEventListener('click',event=>{
  const action=event.target.closest('[data-action]');if(action&&!action.disabled)openForm(action.dataset.action,action.dataset.id);
  const history=event.target.closest('[data-history]');if(history){customerFilter=history.dataset.history;location.hash='history';if(page==='history')render();}
  if(event.target.closest('[data-clear-history]')){customerFilter=null;render();}
});
$('create-button').addEventListener('click',()=>openForm(pages[page][4]));
for(const id of ['close-dialog','cancel'])$(id).addEventListener('click',()=>{if(!saving)$('dialog').close();});
$('dialog').addEventListener('cancel',event=>{if(saving)event.preventDefault();});
$('search').addEventListener('input',render);$('type').addEventListener('change',render);$('available').addEventListener('change',render);
$('group').addEventListener('click',()=>{grouped=!grouped;$('group').setAttribute('aria-pressed',String(grouped));render();});
$('refresh').addEventListener('click',refresh);$('retry').addEventListener('click',refresh);window.addEventListener('hashchange',navigate);
navigate();refresh();
