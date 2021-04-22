config.devServer = config.devServer || {};
config.devServer.port = 3000;
config.devServer.proxy = {
    '/mgtapi': {
        changeOrigin: true,
        pathRewrite: { '^/mgtapi': '' },
        target: 'http://localhost:9911',
    }
};
