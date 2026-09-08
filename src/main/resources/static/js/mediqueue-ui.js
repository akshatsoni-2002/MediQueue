(function(){
"use strict";

function updateUnread(){
 fetch("/notifications/unread",{credentials:"same-origin"})
 .then(function(r){return r.ok?r.text():null})
 .then(function(c){
   if(c===null)return;
   var n=parseInt(c,10)||0;
   document.querySelectorAll("[data-mq-unread]").forEach(function(e){
     e.textContent=n>99?"99+":String(n);
     e.style.display=n>0?"inline-grid":"none";
   });
 }).catch(function(){});
}

function applyTheme(theme){
 document.documentElement.setAttribute("data-theme",theme);
 document.querySelectorAll("[data-mq-theme-icon]").forEach(function(e){
   e.textContent=theme==="dark"?"☀":"☾";
 });
 document.querySelectorAll("[data-mq-theme-label]").forEach(function(e){
   e.textContent=theme==="dark"?"Light mode":"Dark mode";
 });
 localStorage.setItem("mediqueue-theme",theme);
}

window.mqToggleTheme=function(){
 var current=document.documentElement.getAttribute("data-theme")||"light";
 applyTheme(current==="dark"?"light":"dark");
};

window.mqGoBack=function(){
 if(document.referrer && new URL(document.referrer).origin===location.origin && history.length>1){
   history.back();
 }else{
   location.href="/dashboard";
 }
};

window.mqToggleMenu=function(){
 var m=document.getElementById("mqMenuPanel");
 if(m)m.classList.toggle("show");
};

document.addEventListener("click",function(e){
 var m=document.getElementById("mqMenuPanel"),b=document.getElementById("mqMenuButton");
 if(m&&b&&!m.contains(e.target)&&!b.contains(e.target))m.classList.remove("show");
});

function toast(msg){
 var s=document.getElementById("mqToastStack");
 if(!s)return;
 var t=document.createElement("div");
 t.className="mq-toast";
 t.innerHTML='<div class="mq-toast-row"><div class="mq-toast-icon">🔔</div><div style="flex:1;min-width:0"><div class="mq-toast-title">New hospital alert</div><div class="mq-toast-text"></div></div><button type="button" class="mq-toast-close" aria-label="Close">×</button></div>';
 t.querySelector(".mq-toast-text").textContent=msg;
 t.querySelector(".mq-toast-close").onclick=function(){t.remove()};
 s.appendChild(t);
 setTimeout(function(){
   t.style.opacity="0";t.style.transform="translateX(25px)";
   setTimeout(function(){t.remove()},250);
 },5800);
 updateUnread();
}

function connect(){
 if(!window.EventSource)return;
 try{
   var es=new EventSource("/notifications/stream");
   es.addEventListener("notification",function(e){toast(e.data||"You have a new notification.");});
 }catch(e){}
}

document.addEventListener("DOMContentLoaded",function(){
 var header=document.querySelector(".dashboard-header");
 if(header && !header.querySelector(".mq-live-strip")){
   var strip=document.createElement("div");
   strip.className="mq-live-strip";
   strip.innerHTML='<span class="mq-live-pill"><i></i> System online</span><span class="mq-live-pill">⚡ Real-time sync</span><span class="mq-live-pill">🔒 Secure session</span>';
   header.querySelector(".container")?.appendChild(strip);
 }
 var saved=localStorage.getItem("mediqueue-theme");
 applyTheme(saved==="dark"?"dark":"light");
 updateUnread();
 setInterval(updateUnread,15000);
 connect();
 document.body.classList.add("mq-page-enter");
});
})();