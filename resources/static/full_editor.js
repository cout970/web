window.onload = () => {
    const htmlCodeArea = document.getElementById("html-code");
    const cssCodeArea = document.getElementById("css-code");
    const resultArea = document.getElementById("result");
    const logArea = document.getElementById("log");
    const waitTime = 100;
    let lastChange = 0;

    const loadCode = (key) => {
        let code = window.localStorage.getItem(key)
        if (code == undefined) code = ""
        return code
    }

    const saveCode = () => {
        window.localStorage.setItem("html-code", htmlCodeArea.value)
        window.localStorage.setItem("css-code", cssCodeArea.value)
    }

    const runCode = () => {
        let content = (resultArea.contentWindow || resultArea.contentDocument)
        if (content.document) {
            content = content.document
        }
        content.head.innerHTML = '<style>' + cssCodeArea.value + '</style>';
        content.body.innerHTML = htmlCodeArea.value;
    }

    const customPrint = (msg) => {
        logArea.value += `${msg}\n`
    }

    htmlCodeArea.onkeydown = (event) => {
        saveCode()
        lastChange = Date.now()
        setTimeout(() => {
            let now = Date.now()
            if ((now - lastChange) >= waitTime) {
                runCode();
            }
        }, waitTime)

        return insertTab(htmlCodeArea, event)
    }
    cssCodeArea.onkeydown = (event) => {
        saveCode()
        lastChange = Date.now()
        setTimeout(() => {
            let now = Date.now()
            if ((now - lastChange) >= waitTime) {
                runCode();
            }
        }, waitTime)

        return insertTab(cssCodeArea, event)
    }

    htmlCodeArea.value = loadCode('html-code')
    cssCodeArea.value = loadCode('css-code')
    runCode()
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