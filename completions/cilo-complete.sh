#!/usr/bin/env bash
__cilo_complete_wrapper() {
    local current previous doubleprevious tripleprevious options
    COMPREPLY=()
    current="${COMP_WORDS[COMP_CWORD]}"
    previous="${COMP_WORDS[COMP_CWORD-1]}"
    if [ "$COMP_CWORD" -gt 1 ]; then
        doubleprevious="${COMP_WORDS[COMP_CWORD-2]}" > /dev/null 2> /dev/null
        if [ "$COMP_CWORD" -gt 2 ]; then
            tripleprevious="${COMP_WORDS[COMP_CWORD-2]}" > /dev/null 2> /dev/null
        fi
    fi
    options=()
    # AC for secret type
    case "$tripleprevious" in
        secret)
            case "$doubleprevious" in
                create)
                    options="string file input"
                    COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
                    return 0
                ;;
                update)
                    options="string file input"
                    COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
                    return 0
                ;;

            esac
            ;;
    esac

    case "$doubleprevious" in
        secret)
            CILO_SECRET_DIRECTORY="$HOME/.cilo/secret"
            local secrets=""
            for file in ${CILO_SECRET_DIRECTORY}/local/*.enc; do
                if ! [ -d "$file" ]; then
                    if ! [ "$file" = '*' ]; then
                        dirname=`dirname $file`
                        basename=`basename $file`
                        extension="${basename##*.}"
                        filename="${basename%.*}"
                        secrets="${secrets} ${filename}"
                    fi
                fi
            done
            case "$previous" in
                "read")
                    options="$secrets"
                    COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
                    return 0
                    ;;
                update)
                    options="$secrets"
                    COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
                    return 0
                    ;;
                delete)
                    options="$secrets"
                    COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
                    return 0
                    ;;
            esac
        ;;
    esac
            
    
    # AC for Options with arguments
    case "$previous" in
        # -d|--docker-socket)
        #     options=""
        #     return 0
        #     ;;
        -i|--image)
            options=""
            return 0
            ;;
        -l|--library)
            options=""
            return 0
            ;;
        # --log-directory)
        #     options=""
        #     return 0
        #     ;;
        # -r|--registry)
        #     options=""
        #     return 0
        #     ;;
        -s|--server)
            options=""
            return 0
            ;;
        -u|--url-library)
            options=""
            return 0
            ;;
    esac
    
    # AC for sub commands
    case "$previous" in
        run)
            options=""
            COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
            return 0
            ;;
        help)
            options="run help version shell secret"
            COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
            return 0
            ;;
        secret)
            options="list create read update delete"
            COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
            return 0
            ;;
    esac
    
    # AC for current token
    case "$current" in
        -*)
            options="-d --docker-socket -i --image -h --help -l --library --pull -r --registry -s --server -u --url-library -q --quite"
            COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
            return 0
            ;;
        *)
            options="run version shell secret help"
            COMPREPLY=( $(compgen -W "${options}" -- ${current}) )
            return 0
            ;;
    esac
}
complete -o bashdefault -o default -o nospace -F __cilo_complete_wrapper cilo

