var logs = {
    controller: function() {},
    view: function() {}
}

logs.Log = function(data) {
    this.message = m.prop(data);
};

logs.LogList = Array;

logs.vm = {
    init: function() {
        logs.vm.list = new logs.LogList();
        logs.vm.add = function(data) {
            if (logs.vm.list.length >= logs.vm.bufferSize()){
                logs.vm.list.shift();
            }
            logs.vm.list.push(new logs.Log(data));
        };
        logs.vm.stream = function(){
            streamOfLogs(logs.vm.minLogLevel());
        };
        this.minLogLevel = m.prop('TRACE');
        this.logLevelOptions = m.prop([
            { value: 'TRACE', text: 'TRACE' },
            { value: 'DEBUG', text: 'DEBUG' },
            { value: 'INFO', text: 'INFO' },
            { value: 'WARN', text: 'WARN' },
            { value: 'ERROR', text: 'ERROR' }
        ]);
        this.followLogs = m.prop(true);
        this.bufferSize = m.prop(500);
        this.sseSource = m.prop(null);
        logs.vm.getLastErrors = function(){
            m.request({method: "GET", url: "/logs/lastErrors"})
              .then(function(list) {
                   console.dir(list);
              });
        };
    }
};

function streamOfLogs(minLevel) {
    if (logs.vm.sseSource() != null){
        logs.vm.sseSource().close();
    }
    var source = new EventSource("logs/tail?minLevel="+minLevel);
    source.addEventListener('log', function(e) {
        logs.vm.add(JSON.parse(e.data).htmlLog);
        m.render(document.body, logs.view());
        if (logs.vm.followLogs()){
            window.scrollTo(0,document.body.scrollHeight);
        }
    }, false);

    source.addEventListener('open', function(e) {
        logs.vm.add("</br><i>Waiting for logs with min level " + minLevel + " ... </i></br></br>");
         m.render(document.body, logs.view());
    }, false);

    source.addEventListener('error', function(e) {
        console.log("Stream of logs error");
        if (e.readyState == EventSource.CLOSED) {
            logs.vm.add("<i>Log stream closed</i></br></br>");
            console.log("EventSource.CLOSED");
        } else {
            console.dir(e);
        }
    }, false);
    logs.vm.sseSource(source);
}

// Init stuff

var menu = function() {
    return m("div",{class: "pure-u-1"}, [
        m("div",{class: "main-menu pure-menu pure-menu-horizontal"}, [
            m("a[href='/logs']",{class: "pure-menu-heading pure-menu-link"}, "Lazytail"),
            m("ul", {class: "pure-menu-list"},[
                m("li", {class: "pure-menu-item"}, [
                    m("label","display buffer size: "),
                    m("input[type=text]" , {onchange: m.withAttr("value", logs.vm.bufferSize), value: logs.vm.bufferSize()}),
                    m("label","follow logs: "),
                    m("input[type=checkbox]", {onclick: m.withAttr("checked", logs.vm.followLogs), checked: logs.vm.followLogs()}),
                    m("label","min log level: "),
                    m("select", {
                        onchange: function(){
                            logs.vm.minLogLevel(this.value);
                            logs.vm.stream();
                        },
                        value: logs.vm.minLogLevel }, [
                        logs.vm.logLevelOptions().map(function(opt) {
                            return m('option', {value: opt.value}, opt.text);
                        })
                    ])
                ]),
                m("li", {class: "pure-menu-item"}, [
                    m("a[href='/logs/lastErrors.html'][target='_blank']",{class: "pure-menu-link"}, "last errors"),
                ])
            ])
        ])
    ]);
}

logs.view = function() {
    return m("div", {class: "pure-g"}, [
        menu(),
        m("div",{class: "main-content pure-u-1"}, [
            logs.vm.list.map(function(log) {
                return m("div", m.trust(log.message()))
            })
        ])
    ])
};

logs.controller = function() {
    logs.vm.init()
}

m.mount(document.body, {controller: logs.controller, view: logs.view});
logs.vm.stream();