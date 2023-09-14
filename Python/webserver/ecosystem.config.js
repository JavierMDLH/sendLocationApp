module.exports = {
  apps: [
    {
      script: 'server.js',
      watch: '.'
    },
    {
      script: './service-worker/',
      watch: ['./service-worker']
    },
    {
      script: 'sync.sh' // Agrega el archivo shell aquí
    }
  ],
  deploy: {
    production: {
      user: 'SSH_USERNAME',
      host: 'SSH_HOSTMACHINE',
      ref: 'origin/master',
      repo: 'GIT_REPOSITORY',
      path: 'DESTINATION_PATH',
      'pre-deploy-local': '',
      'post-deploy': 'npm install && pm2 reload ecosystem.config.js --env production',
      'pre-setup': ''
    }
  }
};