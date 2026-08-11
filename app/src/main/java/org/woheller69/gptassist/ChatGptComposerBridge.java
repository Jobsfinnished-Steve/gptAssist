package org.woheller69.gptassist;

import android.net.Uri;
import android.webkit.WebView;

import org.json.JSONObject;

public final class ChatGptComposerBridge {
    private ChatGptComposerBridge() {}

    public static boolean append(WebView webView, String context) {
        String url = webView.getUrl();
        String host = url == null ? null : Uri.parse(url).getHost();
        if (host == null || !(host.equals("chatgpt.com") || host.endsWith(".chatgpt.com"))) return false;
        String quoted = JSONObject.quote(context);
        String js = "(()=>{const block=" + quoted + ";const e=document.querySelector('#prompt-textarea,textarea,[contenteditable=\\\"true\\\"]');"
                + "if(!e)return false;const old=('value'in e?e.value:e.innerText)||'';if(old.includes(block))return true;"
                + "const next=old+(old?'\\n\\n':'')+block;if('value'in e)e.value=next;else e.innerText=next;"
                + "e.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:block}));"
                + "e.dispatchEvent(new Event('change',{bubbles:true}));e.focus();return true})()";
        webView.evaluateJavascript(js, null);
        return true;
    }
}
