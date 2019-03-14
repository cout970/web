window.onload = () => {
    const codeArea = document.getElementById("code");
    const resultArea = document.getElementById("result");
    const logArea = document.getElementById("log");
    const waitTime = 1000;
    let lastChange = 0;

    const loadCode = () => {
        let code = window.localStorage.getItem("code")
        if (code == undefined) code = ""
        return code
    }

    const saveCode = () => {
        window.localStorage.setItem("code", codeArea.value)
    }

    const runCode = () => {
        let result;
        let classes;

        const log = console.log;
        console.log = customPrint;
        logArea.value = '';

        try {
            result = eval(codeArea.value)

            if (result === undefined) {
                result = "undefined";
            } else if (result === null) {
                result = "null";
            } else {
                result = result.toString()
            }
            classes = "success";
        } catch (e) {
            result = e.toString()
            classes = "error";
        }

        console.log = log;
        resultArea.className = classes;
        resultArea.textContent = result;
        auto_grow(resultArea)
        auto_grow(logArea)
    }

    const customPrint = (msg) => {
        logArea.value += `${msg}\n`
    }

    codeArea.onkeyup = () => {
        auto_grow(codeArea)
    }

    codeArea.onkeydown = (event) => {
        auto_grow(codeArea)
        saveCode()
        lastChange = Date.now()
        setTimeout(() => {
            let now = Date.now()
            if ((now - lastChange) >= waitTime) {
                runCode();
            }
        }, waitTime)

        return insertTab(codeArea, event)
    }

    codeArea.value = loadCode()
    auto_grow(codeArea)
    auto_grow(resultArea)
    auto_grow(logArea)
    runCode()
}

// https://stackoverflow.com/questions/17772260/textarea-auto-height
function auto_grow(element) {
    element.style.height = "5px";
    element.style.height = (element.scrollHeight - 20) + "px";
}

// https://sumtips.com/snippets/javascript/tab-in-textarea/
function insertTab(o, e) {
    const kC = e.keyCode ? e.keyCode : e.charCode ? e.charCode : e.which;
    if (kC == 9 && !e.shiftKey && !e.ctrlKey && !e.altKey) {
        const oS = o.scrollTop;
        if (o.setSelectionRange) {
            const sS = o.selectionStart;
            const sE = o.selectionEnd;
            o.value = o.value.substring(0, sS) + "    " + o.value.substr(sE);
            o.setSelectionRange(sS + 4, sS + 4);
            o.focus();
        } else if (o.createTextRange) {
            document.selection.createRange().text = "    ";
            e.returnValue = false;
        }
        o.scrollTop = oS;
        if (e.preventDefault) {
            e.preventDefault();
        }
        return false;
    }
    return true;
}