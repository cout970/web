var eval_counter = 1;
document.querySelectorAll('.code-textarea')
    .forEach((i, index) => {
        i.addEventListener('input', autosize, false);
        i.addEventListener('input', save(index), false);
        i.addEventListener('keydown', evaluate_block(index), false);
        setTimeout(() => update_size(i), 0);
    });

document.querySelectorAll('.add-block')
    .forEach(i => {
        i.addEventListener('click', add_block);
    });
document.querySelectorAll('.clear-block')
    .forEach(i => {
        i.addEventListener('click', clear_blocks);
    });

function autosize(event) {
    if (event.target.tagName.toLowerCase() !== 'textarea') return;
    setTimeout(() => update_size(event.target), 0);
}

function update_size(textarea) {
    const lines = textarea.value.split('\n').length;
    textarea.style.height = (lines * 1.165) + 'em';
}

function save(index) {
    return () => {
        const value = document.querySelectorAll('.code-textarea')[index].value;
        localStorage.setItem('code-' + index, value)
    }
}

function load() {
    let blocks = document.querySelectorAll('.code-textarea');
    for (let index = 0; index < 100; index++) {
        const code = localStorage.getItem('code-' + index);
        if (code === undefined || code === null) break;
        if (blocks[index] === undefined) {
            add_block();
            blocks = document.querySelectorAll('.code-textarea');
        }
        blocks[index].value = code;
    }
}

function evaluate_block(index) {
    return (event) => {
        if (event.key == 'Enter' && event.ctrlKey) {
            evaluate_code(index);
        }
    }
}

function evaluate_code(index) {
    const block = document.querySelectorAll('.code-textarea')[index];
    const result = document.querySelectorAll('.result-row')[index];
    const counter = document.querySelectorAll('.index')[index];

    let output = '';
    let log = '';
    let keep = console.log;

    console.log = (msg) => {
        log += msg
    };

    try {
        output = eval(block.value)
    } catch (e) {
        output = e;
    } finally {
        console.log = keep;
    }

    counter.textContent = eval_counter++;
    result.textContent = (output === undefined) ? log : output.toString();
}

function add_block() {
    const wrapper = document.querySelector('.main-wrapper');
    let baseIndex = document.querySelectorAll('.code-textarea').length;
    const child = document.createElement('div');
    child.className = 'code-block';
    child.innerHTML = `
            <div class="code-row">
                <div class="eval-index-wrapper"><span class="eval-index">In [<span class="index"> </span>]:</span></div>
                <textarea class="code-textarea" spellcheck="false" name="code"></textarea>
            </div>
            <div class="result-row">`;
    wrapper.appendChild(child);

    child.querySelectorAll('.code-textarea')
        .forEach((i, index) => {
            index = index + baseIndex;
            i.addEventListener('input', autosize, false);
            i.addEventListener('input', save(index), false);
            i.addEventListener('keydown', evaluate_block(index), false);
            setTimeout(() => update_size(i), 0);
        });
}

function clear_blocks() {
    for (let i = 0; i < 100; i++) {
        localStorage.removeItem('code-' + i);
    }
    const wrapper = document.querySelector('.main-wrapper');
    wrapper.innerHTML = '';
    add_block();
}

add_block();
load();