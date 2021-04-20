const webpack = require("webpack");

config.plugins.push(new webpack.EnvironmentPlugin({
    "MGT_API": "http://localhost:9911",
}))
