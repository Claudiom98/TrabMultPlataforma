const express = require('express');
const ap = express();
const sql = require('./sql.js');
const jwt = require('jsonwebtoken');
const bc = require('bcrypt');
const {loadEnvFile} = require('node:process');
loadEnvFile('./.env');

ap.use(express.json());


const checkToken = function (req,res,next){
    const header = req.headers['authorization'];
    const token = header&&header.split(' ')[1];

    if(token==null){
        return res.status(401).json({error: 'Token não encontrado'});
    } else {
        jwt.verify(token,process.env.ACCESS_TOKEN_SECRET,function(err,usuario){
            if(err){
                return res.status(403).json({error:'Token expirado'});
            } else{
                req.usuario = usuario;
                next();
            }
        });
    }
}
ap.post('/',async function (req,res) {
    try {
        const show = await sql.usuarioExistente(req.body);
        res.status(201).json(show.rows);
        
        
    } catch (err) {
        console.log(err);
    }
});

ap.post('/inserir',checkToken, async function (req,res) {
    try {
        await sql.inserirLocal(req.body,req.usuario.id);
        res.status(201).json({message: 'ok'});
    } catch (err) {
        console.log(err); 
        es.status(500).json({error:'Erro do servidor'});  
}
});

ap.get('/locais/pendentes',checkToken,async function (req,res) {
    try {
        const show = await sql.pendentes();
        res.status(201).json(show);
    } catch (err) {
        console.log(err);
        res.status(500).json({error:'Erro do servidor'});
    }
    
});

ap.put('/locais/:id/limpar', checkToken,async function (req,res) {
    try {
        await sql.limpar(req.params.id,req.usuario.id);
        res.status(201).json({message: 'ok'});
    } catch (err) {
        console.log(err);
        res.status(500).json({error:'Erro do servidor'});
    }
});

ap.post('/usuarios/cadastro',async function(req,res){
    try {
        if(!req.body.email || !req.body.senha){
            return res.status(400).json({ error: 'Email e senha são obrigatórios.'});
        } else{
            const existente = await sql.usuarioExistente(req.body);
            if(existente.rows.length > 0){
                res.status(400).json({error:'Este email ja esta registrado'});
            }else{
                await sql.inserirUsuario(req.body);
                res.status(201).json({message: 'ok'})
            }
            
        }
    } catch (err) {
        console.log(err);
        res.status(500).json({error: 'Erro do servidor'});
    }
});

ap.post('/usuarios/login',async function(req,res) {
    try {
        const existente = await sql.usuarioExistente(req.body);
        if(existente.rows.length === 0){
            res.status(401).json({error: 'Email ou senha invalidos'});
        } else{
            const senhaCorreta = await bc.compare(req.body.senha,existente.rows[0].password_hash);
            if(!senhaCorreta){
                res.status(401).json({error: 'Email ou senha invalidos'});
            } else{
                 const payload={
                id: existente.rows[0].id,
                email:existente.rows[0].email
            };
            
            const token = jwt.sign(
                payload,
                process.env.ACCESS_TOKEN_SECRET,
                { expiresIn:'6h'}
            );

            res.status(201).json({
                message: 'Sucesso no login',
                token: token,
                usuario: payload
            });
            }
           
        }

        
    } catch (err) {
        console.log(err);
        res.status(500).json({error: 'Erro do servidor'});
    }
});

ap.listen(3000);