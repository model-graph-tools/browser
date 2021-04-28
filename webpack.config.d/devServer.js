config.devServer = config.devServer || {};
config.devServer.headers = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "*",
    "Access-Control-Allow-Headers": "*"
};
config.devServer.port = 3000;
config.devServer.proxy = {
    '/mgtapi': {
        changeOrigin: true,
        pathRewrite: { '^/mgtapi': '' },
        target: 'http://localhost:9911',
    }
};
